package com.talex.server.specifications;

import com.talex.server.entities.KycSession;
import com.talex.server.enums.KycStatus;
import com.talex.server.utils.ValidationUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KycSessionSpec {

    public static Specification<KycSession> filterByCriteria(
            Map<String, Object> criteria,
            KycStatus[] statuses) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicateList = new ArrayList<>();

            // Terms Version
            String termsVersion = (String) criteria.get("termsVersion");
            if (!ValidationUtils.isNullOrEmpty(termsVersion)) {
                predicateList.add(criteriaBuilder.like(root.get("termsVersion"), "%" + termsVersion + "%"));
            }

            // KYC Status
            if (statuses != null && statuses.length > 0) {
                predicateList.add(root.get("status").in((Object[]) statuses));
            }

            // Started At - From Date
            String startedAtFrom = (String) criteria.get("startedAtFrom");
            if (!ValidationUtils.isNullOrEmpty(startedAtFrom)) {
                predicateList.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startedAt"), LocalDateTime.parse(startedAtFrom)));
            }

            // Started At - To Date
            String startedAtTo = (String) criteria.get("startedAtTo");
            if (!ValidationUtils.isNullOrEmpty(startedAtTo)) {
                predicateList.add(criteriaBuilder.lessThanOrEqualTo(root.get("startedAt"), LocalDateTime.parse(startedAtTo)));
            }

            // Completed At - From Date
            String completedAtFrom = (String) criteria.get("completedAtFrom");
            if (!ValidationUtils.isNullOrEmpty(completedAtFrom)) {
                predicateList.add(criteriaBuilder.greaterThanOrEqualTo(root.get("completedAt"), LocalDateTime.parse(completedAtFrom)));
            }

            // Completed At - To Date
            String completedAtTo = (String) criteria.get("completedAtTo");
            if (!ValidationUtils.isNullOrEmpty(completedAtTo)) {
                predicateList.add(criteriaBuilder.lessThanOrEqualTo(root.get("completedAt"), LocalDateTime.parse(completedAtTo)));
            }

            // Creator ID (for N-1 relationship with Creator)
            String creatorId = (String) criteria.get("creatorId");
            if (!ValidationUtils.isNullOrEmpty(termsVersion)) {
                predicateList.add(criteriaBuilder.equal(root.get("creator").get("id"), creatorId));
            }

            Predicate[] predicates = predicateList.toArray(new Predicate[0]);
            return criteriaBuilder.and(predicates);
        };
    }
}
