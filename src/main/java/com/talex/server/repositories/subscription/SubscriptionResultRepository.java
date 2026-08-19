package com.talex.server.repositories.subscription;

import com.talex.server.entities.subscription.SubscriptionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionResultRepository extends JpaRepository<SubscriptionResult, String> {
    Optional<SubscriptionResult> findByMonthYear(String monthYear);
}