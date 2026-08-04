package com.talex.server.repositories;

import com.talex.server.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, String>, JpaSpecificationExecutor<Notification> {

    // Đếm số lượng thông báo chưa đọc của User
    long countByRecipientIdAndIsReadFalse(String recipientId);
}