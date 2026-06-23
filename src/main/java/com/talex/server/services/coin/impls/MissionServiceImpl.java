package com.talex.server.services.coin.impls;

import com.talex.server.dtos.requests.coin.MissionRequestDto;
import com.talex.server.dtos.responses.coin.MissionProgressResponseDto;
import com.talex.server.entities.coin.Mission;
import com.talex.server.entities.coin.UserMissionProgress;
import com.talex.server.exceptions.codes.CoinErrorCode;
import com.talex.server.exceptions.details.CoinException;
import com.talex.server.repositories.coin.MissionRepository;
import com.talex.server.repositories.coin.UserMissionProgressRepository;
import com.talex.server.services.coin.ICoinWalletService;
import com.talex.server.services.coin.IMissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MissionServiceImpl implements IMissionService {

    private static final String HEARTBEAT_HASH_KEY = "mission:online_heartbeat";

    private final MissionRepository missionRepository;
    private final UserMissionProgressRepository userMissionProgressRepository;
    private final ICoinWalletService coinWalletService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<MissionProgressResponseDto> getMyDailyMissions(UUID accountId) {
        LocalDate today = LocalDate.now();
        List<Mission> activeMissions = missionRepository.findByIsActiveTrue();
        List<UserMissionProgress> todayProgress =
                userMissionProgressRepository.findByAccountIdAndProgressDate(accountId, today);

        Map<UUID, UserMissionProgress> progressByMissionId = todayProgress.stream()
                .collect(Collectors.toMap(
                        UserMissionProgress::getMissionId,
                        Function.identity()
                ));

        return activeMissions.stream()
                .map(mission -> {
                    UserMissionProgress progress = progressByMissionId.get(mission.getMissionId());

                    return MissionProgressResponseDto.builder()
                            .missionId(mission.getMissionId())
                            .code(mission.getCode())
                            .title(mission.getTitle())
                            .description(mission.getDescription())
                            .rewardAmount(mission.getRewardAmount())
                            .targetValue(mission.getTargetValue())
                            .currentValue(progress == null ? 0 : progress.getCurrentValue())
                            .isCompleted(progress != null && progress.isCompleted())
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addProgress(UUID accountId, String missionCode, int increment) {
        missionRepository.findByCode(missionCode)
                .ifPresent(mission -> processProgressInternal(accountId, mission, increment));
    }

    private void processProgressInternal(UUID accountId, Mission mission, int increment) {
        LocalDate today = LocalDate.now();
        UserMissionProgress progress = userMissionProgressRepository
                .findByAccountIdAndMissionIdAndProgressDate(accountId, mission.getMissionId(), today)
                .orElseGet(() -> UserMissionProgress.builder()
                        .accountId(accountId)
                        .missionId(mission.getMissionId())
                        .progressDate(today)
                        .currentValue(0)
                        .isCompleted(false)
                        .build());

        if (progress.isCompleted()) return;

        int updatedValue = progress.getCurrentValue() + increment;
        progress.setCurrentValue(updatedValue);

        if (updatedValue >= mission.getTargetValue()) {
            progress.setCurrentValue(mission.getTargetValue());
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());

            progress = userMissionProgressRepository.save(progress);

            coinWalletService.creditCoin(accountId, mission.getRewardAmount(), "MISSION_REWARD",
                    progress.getProgressId().toString(), "Hoàn thành nhiệm vụ: " + mission.getTitle());
        } else {
            userMissionProgressRepository.save(progress);
        }
    }

    @Override
    public void processOnlineHeartbeat(UUID accountId) {
        stringRedisTemplate.opsForHash().increment(
                HEARTBEAT_HASH_KEY,
                accountId.toString(),
                1
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void distributeOnlineHeartbeat(UUID accountId, int minutes) {
        List<Mission> activeMissions = missionRepository.findByIsActiveTrue();
        List<Mission> onlineMissions = activeMissions.stream()
                .filter(mission -> mission.getCode() != null
                        && mission.getCode().toUpperCase().startsWith("ONLINE_"))
                .toList();

        for (Mission mission : onlineMissions) {
            processProgressInternal(accountId, mission, minutes);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Mission> getAllMissionsForAdmin() {
        return missionRepository.findAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Mission createMission(MissionRequestDto request, String adminId) {
        rejectDuplicateCode(request.getCode(), null);

        Mission mission = Mission.builder()
                .code(request.getCode())
                .title(request.getTitle())
                .description(request.getDescription())
                .rewardAmount(request.getRewardAmount())
                .targetValue(request.getTargetValue())
                .isActive(request.isActive())
                .build();
        mission.markCreatedBy(adminId);

        return missionRepository.save(mission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Mission updateMission(UUID missionId, MissionRequestDto request, String adminId) {
        Mission mission = getMissionOrThrow(missionId);
        rejectDuplicateCode(request.getCode(), missionId);

        mission.setCode(request.getCode());
        mission.setTitle(request.getTitle());
        mission.setDescription(request.getDescription());
        mission.setRewardAmount(request.getRewardAmount());
        mission.setTargetValue(request.getTargetValue());
        mission.setActive(request.isActive());
        mission.markUpdatedBy(adminId);

        return missionRepository.save(mission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Mission toggleMissionStatus(UUID missionId, String adminId) {
        Mission mission = getMissionOrThrow(missionId);
        mission.setActive(!mission.isActive());
        mission.markUpdatedBy(adminId);

        return missionRepository.save(mission);
    }

    private Mission getMissionOrThrow(UUID missionId) {
        return missionRepository.findById(missionId)
                .orElseThrow(() -> new CoinException(CoinErrorCode.MISSION_NOT_FOUND));
    }

    private void rejectDuplicateCode(String code, UUID currentMissionId) {
        missionRepository.findByCode(code)
                .filter(existing -> !existing.getMissionId().equals(currentMissionId))
                .ifPresent(existing -> {
                    throw new CoinException(CoinErrorCode.MISSION_CODE_ALREADY_EXISTS);
                });
    }
}
