package com.talex.server.specifications.report;

import com.talex.server.entities.report.Appeal;
import com.talex.server.enums.report.AppealStatus;
import com.talex.server.utils.SpecUtils;
import com.talex.server.utils.ValidationUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AppealSpec {

    public static Specification<Appeal> filterByCriteria(Map<String, Object> criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria != null && !criteria.isEmpty()) {
                String status = (String) criteria.get("status");
                if (!ValidationUtils.isNullOrEmpty(status)) {
                    predicates.add(builder.equal(root.get("status"), AppealStatus.valueOf(status)));
                }

                String appellantId = (String) criteria.get("appellantId");
                if (!ValidationUtils.isNullOrEmpty(appellantId)) {
                    predicates.add(builder.equal(root.get("appellantId"), appellantId));
                }

                String reviewerId = (String) criteria.get("reviewerId");
                if (!ValidationUtils.isNullOrEmpty(reviewerId)) {
                    predicates.add(builder.equal(root.get("reviewerId"), reviewerId));
                }

                SpecUtils.addAuditDateFilters(root, builder, predicates, criteria);
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}