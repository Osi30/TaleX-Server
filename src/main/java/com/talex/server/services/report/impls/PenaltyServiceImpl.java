package com.talex.server.services.report.impls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.report.response.PenaltyResponseDto;
import com.talex.server.entities.report.Penalty;
import com.talex.server.enums.report.AuditActionType;
import com.talex.server.enums.report.PenaltyStatus;
import com.talex.server.exceptions.codes.report.ModerationErrorCode;
import com.talex.server.exceptions.details.report.ModerationException;
import com.talex.server.mappers.report.PenaltyMapper;
import com.talex.server.repositories.report.PenaltyRepository;
import com.talex.server.services.report.PenaltyService;
import com.talex.server.specifications.report.PenaltySpec;
import com.talex.server.utils.PageUtils;
import io.questdb.client.Sender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PenaltyServiceImpl implements PenaltyService {

    private final PenaltyRepository penaltyRepository;
    private final PenaltyMapper penaltyMapper;
    private final Sender questDBSender;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<PenaltyResponseDto> getMyPenalties(String currentUserId, BaseFilterRequestDto filterRequest) {
        Pageable pageable = PageUtils.buildPageable(
                filterRequest.getPage(), filterRequest.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Penalty> pageResult = penaltyRepository.findByTargetUserId(currentUserId, pageable);
        List<PenaltyResponseDto> content = pageResult.stream().map(penaltyMapper::toResponseDto).toList();

        return BasePageResponse.<PenaltyResponseDto>builder()
                .content(content)
                .pageNumber(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .isFirst(pageResult.isFirst())
                .isLast(pageResult.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<PenaltyResponseDto> filterPenalties(BaseFilterRequestDto filterRequest) {
        Pageable pageable = PageUtils.buildPageable(
                filterRequest.getPage(), filterRequest.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Penalty> pageResult = penaltyRepository.findAll(
                PenaltySpec.filterByCriteria(filterRequest.getCriteria()), pageable);

        List<PenaltyResponseDto> content = pageResult.stream().map(penaltyMapper::toResponseDto).toList();

        return BasePageResponse.<PenaltyResponseDto>builder()
                .content(content)
                .pageNumber(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .isFirst(pageResult.isFirst())
                .isLast(pageResult.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PenaltyResponseDto getPenaltyById(String penaltyId) {
        Penalty penalty = penaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new ModerationException(ModerationErrorCode.PENALTY_NOT_FOUND));
        return penaltyMapper.toResponseDto(penalty);
    }

    @Override
    @Transactional
    public PenaltyResponseDto revokePenalty(String penaltyId, String adminId, String role, String reason) {
        Penalty penalty = penaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new ModerationException(ModerationErrorCode.PENALTY_NOT_FOUND));

        penalty.setStatus(PenaltyStatus.REVOKED);
        Penalty updated = penaltyRepository.save(penalty);
        publishPenaltyEvent(penalty);

//        questDBSender.table("penalty_logs")
//                .symbol("penalty_id", penalty.getPenaltyId())
//                .symbol("actor_id", adminId)
//                .symbol("report_role", role)
//                .symbol("action_type", AuditActionType.PENALTY_ISSUED.toString())
//                .symbol("target_type", penalty.getTargetType().toString())
//                .symbol("target_id", penalty.getTargetId())
//                .symbol("payload", "Manual Revoked penalty. Reason: " + reason)
//                .at(Instant.now());

        return penaltyMapper.toResponseDto(updated);
    }

    private void publishPenaltyEvent(Penalty penalty) {
        try {
            String payload = objectMapper.writeValueAsString(penalty);
            kafkaTemplate.send("penalty-event-topic", penalty.getTargetUserId(), payload);
        } catch (Exception e) {
            log.error("Lỗi khi gửi Kafka message cho PenaltyId: {}", penalty.getPenaltyId(), e);
        }
    }
}