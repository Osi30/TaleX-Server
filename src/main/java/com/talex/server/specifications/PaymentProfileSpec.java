package com.talex.server.specifications;

import com.talex.server.entities.creator.PaymentProfile;
import com.talex.server.enums.creator.PaymentProfileStatus;
import com.talex.server.utils.SpecUtils;
import com.talex.server.utils.ValidationUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PaymentProfileSpec {

    public static Specification<PaymentProfile> filterByCriteria(Map<String, Object> criteria, String creatorId) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(builder.equal(root.get("isDeleted"), false));

            // Lọc theo creatorId (bắt buộc)
            if (!ValidationUtils.isNullOrEmpty(creatorId)) {
                predicates.add(builder.equal(root.get("creator").get("creatorId"), creatorId));
            }

            if (criteria == null || criteria.isEmpty()) {
                return builder.and(predicates.toArray(new Predicate[0]));
            }

            // Lọc theo bankCode (partial, case-insensitive)
            String bankCode = (String) criteria.get("bankCode");
            if (!ValidationUtils.isNullOrEmpty(bankCode)) {
                predicates.add(builder.like(builder.lower(root.get("bankCode")), "%" + bankCode.toLowerCase() + "%"));
            }

            // Lọc theo accountNumber (partial, case-insensitive)
            String accountNumber = (String) criteria.get("accountNumber");
            if (!ValidationUtils.isNullOrEmpty(accountNumber)) {
                predicates.add(builder.like(builder.lower(root.get("accountNumber")), "%" + accountNumber.toLowerCase() + "%"));
            }

            // Lọc theo accountName (partial, case-insensitive)
            String accountName = (String) criteria.get("accountName");
            if (!ValidationUtils.isNullOrEmpty(accountName)) {
                predicates.add(builder.like(builder.lower(root.get("accountName")), "%" + accountName.toLowerCase() + "%"));
            }

            // Lọc theo status (exact match)
            String status = (String) criteria.get("status");
            if (!ValidationUtils.isNullOrEmpty(status)) {
                predicates.add(builder.equal(root.get("status"), PaymentProfileStatus.valueOf(status)));
            }

            // Lọc theo isPrimary (exact match)
            String isPrimary = (String) criteria.get("isPrimary");
            if (!ValidationUtils.isNullOrEmpty(isPrimary)) {
                predicates.add(builder.equal(root.get("isPrimary"), Boolean.valueOf(isPrimary)));
            }

            SpecUtils.addAuditDateFilters(root, builder, predicates, criteria);

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
