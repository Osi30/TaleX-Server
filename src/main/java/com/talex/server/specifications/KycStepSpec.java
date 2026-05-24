package com.talex.server.specifications;

import com.talex.server.entities.KycStep;
import com.talex.server.enums.StepType;
import com.talex.server.utils.ValidationUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KycStepSpec {
    public static Specification<KycStep> filterByCriteria(Map<String, Object> criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicateList = new ArrayList<>();

            // isSuccess
            String isSuccess = (String) criteria.get("isSuccess");
            if (!ValidationUtils.isNullOrEmpty(isSuccess)) {
                predicateList.add(criteriaBuilder.equal(root.get("isSuccess"), Boolean.valueOf(isSuccess)));
            }

            // stepType
            String stepType = (String) criteria.get("stepType");
            if (!ValidationUtils.isNullOrEmpty(stepType)) {
                predicateList.add(criteriaBuilder.equal(root.get("stepType"), StepType.valueOf(stepType)));
            }

            // provider
            String provider = (String) criteria.get("provider");
            if (!ValidationUtils.isNullOrEmpty(provider)) {
                predicateList.add(criteriaBuilder.like(root.get("provider"), "%" + provider + "%"));
            }

            // kycSessionId
            String kycSessionId = (String) criteria.get("kycSessionId");
            if (!ValidationUtils.isNullOrEmpty(kycSessionId)) {
                predicateList.add(criteriaBuilder.equal(root.get("kycSession").get("kycSessionId"), kycSessionId));
            }

            Predicate[] predicates = predicateList.toArray(new Predicate[0]);
            return criteriaBuilder.and(predicates);
        };
    }
}
