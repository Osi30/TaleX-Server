package com.talex.server.specifications.report;

import com.talex.server.entities.report.ModerationTicket;
import com.talex.server.enums.report.TargetType;
import com.talex.server.enums.report.TicketStatus;
import com.talex.server.utils.SpecUtils;
import com.talex.server.utils.ValidationUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ModerationTicketSpec {

    public static Specification<ModerationTicket> filterByCriteria(Map<String, Object> criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria != null && !criteria.isEmpty()) {
                String status = (String) criteria.get("status");
                if (!ValidationUtils.isNullOrEmpty(status)) {
                    predicates.add(builder.equal(root.get("status"), TicketStatus.valueOf(status)));
                }

                String targetType = (String) criteria.get("targetType");
                if (!ValidationUtils.isNullOrEmpty(targetType)) {
                    predicates.add(builder.equal(root.get("targetType"), TargetType.valueOf(targetType)));
                }

                String assignedStaffId = (String) criteria.get("assignedStaffId");
                if (!ValidationUtils.isNullOrEmpty(assignedStaffId)) {
                    predicates.add(builder.equal(root.get("assignedStaffId"), assignedStaffId));
                }

                SpecUtils.addAuditDateFilters(root, builder, predicates, criteria);
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}