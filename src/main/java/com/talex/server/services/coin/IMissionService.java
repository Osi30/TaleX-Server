package com.talex.server.services.coin;

import com.talex.server.dtos.requests.coin.MissionRequestDto;
import com.talex.server.dtos.responses.coin.MissionProgressResponseDto;
import com.talex.server.entities.coin.Mission;

import java.util.List;
import java.util.UUID;

public interface IMissionService {

    List<MissionProgressResponseDto> getMyDailyMissions(UUID accountId);

    void addProgress(UUID accountId, String missionCode, int increment);

    void processOnlineHeartbeat(UUID accountId);

    void distributeOnlineHeartbeat(UUID accountId, int minutes);

    List<Mission> getAllMissionsForAdmin();

    Mission createMission(MissionRequestDto request, String adminId);

    Mission updateMission(UUID missionId, MissionRequestDto request, String adminId);

    Mission toggleMissionStatus(UUID missionId, String adminId);
}
