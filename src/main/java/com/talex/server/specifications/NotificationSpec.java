package com.talex.server.specifications;

import com.talex.server.entities.Notification;
import com.talex.server.utils.SpecUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NotificationSpec {

    public static Specification<Notification> filterByCriteria(String recipientId, Map<String, Object> criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(builder.equal(root.get("recipientId"), recipientId));

            if (criteria != null && !criteria.isEmpty()) {
                Boolean isRead = (Boolean) criteria.get("isRead");
                if (isRead != null) {
                    predicates.add(builder.equal(root.get("isRead"), isRead));
                }
            }

            SpecUtils.addAuditDateFilters(root, builder, predicates, criteria);

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}