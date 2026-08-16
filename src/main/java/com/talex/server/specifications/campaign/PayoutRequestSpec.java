package com.talex.server.specifications.campaign;

import com.talex.server.entities.campaign.PayoutRequest;
import com.talex.server.enums.engagement.PayoutRequestStatus;
import com.talex.server.utils.SpecUtils;
import com.talex.server.utils.ValidationUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PayoutRequestSpec {

    public static Specification<PayoutRequest> filterByCriteria(Map<String, Object> criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria != null && !criteria.isEmpty()) {
                // 1. Lọc theo Account ID
                String accountId = (String) criteria.get("accountId");
                if (!ValidationUtils.isNullOrEmpty(accountId)) {
                    predicates.add(builder.equal(root.get("accountId"), UUID.fromString(accountId)));
                }

                // 2. Lọc theo Status (PENDING, APPROVED, REJECTED)
                String status = (String) criteria.get("status");
                if (!ValidationUtils.isNullOrEmpty(status)) {
                    predicates.add(builder.equal(root.get("status"), PayoutRequestStatus.valueOf(status)));
                }

                // 3. Tự động thêm bộ lọc ngày tạo createdAt / updatedAt từ SpecUtils
                SpecUtils.addAuditDateFilters(root, builder, predicates, criteria);
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}