package com.talex.server.specifications.report;

import com.talex.server.entities.report.Penalty;
import com.talex.server.enums.report.PenaltyLevel;
import com.talex.server.enums.report.PenaltyStatus;
import com.talex.server.enums.report.TargetType;
import com.talex.server.utils.SpecUtils;
import com.talex.server.utils.ValidationUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PenaltySpec {

    public static Specification<Penalty> filterByCriteria(Map<String, Object> criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria != null && !criteria.isEmpty()) {
                String targetUserId = (String) criteria.get("targetUserId");
                if (!ValidationUtils.isNullOrEmpty(targetUserId)) {
                    predicates.add(builder.equal(root.get("targetUserId"), targetUserId));
                }

                String status = (String) criteria.get("status");
                if (!ValidationUtils.isNullOrEmpty(status)) {
                    predicates.add(builder.equal(root.get("status"), PenaltyStatus.valueOf(status)));
                }

                String level = (String) criteria.get("level");
                if (!ValidationUtils.isNullOrEmpty(level)) {
                    predicates.add(builder.equal(root.get("level"), PenaltyLevel.valueOf(level)));
                }

                String targetType = (String) criteria.get("targetType");
                if (!ValidationUtils.isNullOrEmpty(targetType)) {
                    predicates.add(builder.equal(root.get("targetType"), TargetType.valueOf(targetType)));
                }

                SpecUtils.addAuditDateFilters(root, builder, predicates, criteria);
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}