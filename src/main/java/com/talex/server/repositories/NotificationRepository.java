package com.talex.server.repositories;

import com.talex.server.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, String>, JpaSpecificationExecutor<Notification> {

    // Đếm số lượng thông báo chưa đọc của User
    long countByRecipientIdAndIsReadFalse(String recipientId);

    // Xoá các thông báo ĐÃ ĐỌC của User — không đụng chưa đọc, tránh làm mất thông báo
    // người dùng chưa kịp xem.
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.recipientId = :recipientId AND n.isRead = true")
    int deleteByRecipientIdAndIsReadTrue(@Param("recipientId") String recipientId);
}