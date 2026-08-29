package com.talex.server.services.impls;

import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.notification.NotificationResponseDto;
import com.talex.server.entities.Notification;
import com.talex.server.mappers.notification.NotificationMapper;
import com.talex.server.repositories.NotificationRepository;
import com.talex.server.services.NotificationService;
import com.talex.server.specifications.NotificationSpec;
import com.talex.server.utils.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<NotificationResponseDto> getMyNotifications(String recipientId, BaseFilterRequestDto filterRequest) {
        Pageable pageable = PageUtils.buildPageable(
                filterRequest.getPage(), filterRequest.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Notification> pageResult = notificationRepository.findAll(
                NotificationSpec.filterByCriteria(recipientId, filterRequest.getCriteria()), pageable);

        List<NotificationResponseDto> content = pageResult.stream().map(notificationMapper::toResponseDto).toList();

        return BasePageResponse.<NotificationResponseDto>builder()
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
    public long getUnreadCount(String recipientId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(recipientId);
    }

    @Override
    @Transactional
    public void markAsRead(String notificationId, String recipientId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông báo"));

        if (notification.getRecipientId().equals(recipientId)) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(String recipientId) {
        List<Notification> unreadNotifications = notificationRepository.findAll(
                NotificationSpec.filterByCriteria(recipientId, java.util.Map.of("isRead", false)));

        unreadNotifications.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    @Override
    @Transactional
    public int deleteReadNotifications(String recipientId) {
        return notificationRepository.deleteByRecipientIdAndIsReadTrue(recipientId);
    }
}