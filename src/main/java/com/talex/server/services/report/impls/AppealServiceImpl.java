package com.talex.server.services.report.impls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.report.request.AppealProcessRequestDto;
import com.talex.server.dtos.report.request.AppealRequestDto;
import com.talex.server.dtos.report.response.AppealResponseDto;
import com.talex.server.entities.Notification;
import com.talex.server.entities.report.Appeal;
import com.talex.server.entities.report.Penalty;
import com.talex.server.enums.NotificationType;
import com.talex.server.enums.report.AppealStatus;
import com.talex.server.enums.report.AuditActionType;
import com.talex.server.enums.report.PenaltyStatus;
import com.talex.server.exceptions.codes.report.ModerationErrorCode;
import com.talex.server.exceptions.details.report.ModerationException;
import com.talex.server.mappers.report.AppealMapper;
import com.talex.server.repositories.report.AppealRepository;
import com.talex.server.repositories.NotificationRepository;
import com.talex.server.repositories.report.PenaltyRepository;
import com.talex.server.services.report.AppealService;
import com.talex.server.specifications.report.AppealSpec;
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
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppealServiceImpl implements AppealService {

    private final AppealRepository appealRepository;
    private final PenaltyRepository penaltyRepository;
    private final NotificationRepository notificationRepository;
    private final AppealMapper appealMapper;
    private final Sender questDBSender;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AppealResponseDto createAppeal(String currentUserId, String role, String penaltyId, AppealRequestDto requestDto) {
        Penalty penalty = penaltyRepository.findByPenaltyIdAndStatus(penaltyId, PenaltyStatus.ACTIVE)
                .orElseThrow(() -> new ModerationException(ModerationErrorCode.PENALTY_NOT_FOUND));

        // 1. Check xem đã từng khiếu nại chưa hoặc đã hết hạn xử lý
        validateAppeal(penalty);

        // 2. Tạo Appeal Entity
        Appeal appeal = Appeal.builder()
                .penalty(penalty)
                .appellantId(currentUserId)
                .reason(requestDto.getReason())
                .proofDocuments(requestDto.getProofDocuments())
                .status(AppealStatus.PENDING)
                .build();
        Appeal savedAppeal = appealRepository.save(appeal);

        // 3. Ghi Audit Log
        questDBSender.table("penalty_logs")
                .symbol("penalty_id", penaltyId)
                .symbol("actor_id", currentUserId)
                .symbol("report_role", role)
                .symbol("action_type", AuditActionType.APPEAL_SUBMITTED.toString())
                .symbol("target_type", penalty.getTargetType().toString())
                .symbol("target_id", penalty.getTargetId())
                .symbol("payload", "Submitted appeal for penalty: " + penaltyId)
                .at(Instant.now());

        return appealMapper.toResponseDto(savedAppeal);
    }

    /// Check xem đã từng khiếu nại chưa
    private void validateAppeal(Penalty penalty){
        if (appealRepository.existsByPenalty_PenaltyId(penalty.getPenaltyId())) {
            throw new ModerationException(ModerationErrorCode.ALREADY_APPEALED);
        }

    }
    @Override
    @Transactional
    public AppealResponseDto processAppeal(String adminId, String role, String appealId, AppealProcessRequestDto requestDto) {
        Appeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new ModerationException(ModerationErrorCode.APPEAL_NOT_FOUND));

        if (appeal.getStatus() != AppealStatus.PENDING) {
            throw new ModerationException(ModerationErrorCode.INVALID_STATUS);
        }

        appeal.setReviewerId(adminId);
        appeal.setAdminNote(requestDto.getAdminNote());
        appeal.setResolvedAt(LocalDateTime.now());

        Penalty penalty = appeal.getPenalty();

        if (requestDto.getIsApproved()) {
            // Admin Chấp nhận Khiếu nại -> Gỡ phạt (Revoke Penalty)
            appeal.setStatus(AppealStatus.APPROVED);
            penalty.setStatus(PenaltyStatus.REVOKED);
            penaltyRepository.save(penalty);
            publishPenaltyEvent(penalty);

            questDBSender.table("penalty_logs")
                    .symbol("penalty_id", penalty.getPenaltyId())
                    .symbol("actor_id", adminId)
                    .symbol("report_role", role)
                    .symbol("action_type", AuditActionType.APPEAL_APPROVED.toString())
                    .symbol("target_type", penalty.getTargetType().toString())
                    .symbol("target_id", penalty.getTargetId())
                    .symbol("payload", "Approved appeal. Penalty revoked.")
                    .at(Instant.now());

            notificationRepository.save(Notification.builder()
                    .recipientId(appeal.getAppellantId())
                    .title("Khiếu nại được chấp nhận")
                    .content("Đơn khiếu nại của bạn đã được Admin chấp nhận. Hình phạt đã được gỡ bỏ.")
                    .type(NotificationType.APPEAL_RESULT)
                    .referenceType("APPEAL")
                    .referenceId(appealId)
                    .build());
        } else {
            // Admin Bác bỏ Khiếu nại -> Giữ nguyên phạt
            appeal.setStatus(AppealStatus.REJECTED);

            questDBSender.table("penalty_logs")
                    .symbol("penalty_id", penalty.getPenaltyId())
                    .symbol("actor_id", adminId)
                    .symbol("report_role", role)
                    .symbol("action_type", AuditActionType.APPEAL_REJECTED.toString())
                    .symbol("target_type", penalty.getTargetType().toString())
                    .symbol("target_id", penalty.getTargetId())
                    .symbol("payload", "Rejected appeal with note: " + requestDto.getAdminNote())
                    .at(Instant.now());

            notificationRepository.save(Notification.builder()
                    .recipientId(appeal.getAppellantId())
                    .title("Khiếu nại bị từ chối")
                    .content("Đơn khiếu nại của bạn đã bị từ chối. Lý do: " + requestDto.getAdminNote())
                    .type(NotificationType.APPEAL_RESULT)
                    .referenceType("APPEAL")
                    .referenceId(appealId)
                    .build());
        }

        Appeal updatedAppeal = appealRepository.save(appeal);
        return appealMapper.toResponseDto(updatedAppeal);
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<AppealResponseDto> filterAppeals(BaseFilterRequestDto filterRequest) {
        Pageable pageable = PageUtils.buildPageable(
                filterRequest.getPage(), filterRequest.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Appeal> pageResult = appealRepository.findAll(
                AppealSpec.filterByCriteria(filterRequest.getCriteria()), pageable);

        List<AppealResponseDto> content = pageResult.stream().map(appealMapper::toResponseDto).toList();

        return BasePageResponse.<AppealResponseDto>builder()
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
    public BasePageResponse<AppealResponseDto> filterAppeals(String accountId, BaseFilterRequestDto filterRequest) {
        filterRequest.getCriteria().put("appellantId", accountId);
        return filterAppeals(filterRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public AppealResponseDto getAppealByPenaltyId(String penaltyId) {
        Appeal appeal = appealRepository.findByPenalty_PenaltyId(penaltyId)
                .orElseThrow(() -> new ModerationException(ModerationErrorCode.APPEAL_NOT_FOUND));

        return appealMapper.toResponseDto(appeal);
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