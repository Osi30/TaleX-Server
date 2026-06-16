package com.talex.server.repositories.subscription;

import com.talex.server.entities.subscription.AccountSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountSubscriptionRepository extends JpaRepository<AccountSubscription, String> {
    Page<AccountSubscription> findAll(Specification<AccountSubscription> example, Pageable pageable);
}
