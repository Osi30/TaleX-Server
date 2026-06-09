package com.talex.server.repositories;

import com.talex.server.entities.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository
        extends JpaRepository<Subscription, String>, JpaSpecificationExecutor<Subscription> {
    Optional<Subscription> findBySubscriptionIdAndIsDeletedFalse(String id);
}
