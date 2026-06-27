package com.talex.server.specifications;

import com.talex.server.entities.creator.CreatorTier;
import com.talex.server.utils.SpecUtils;
import com.talex.server.utils.ValidationUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreatorTierSpec {

    public static Specification<CreatorTier> filterByCriteria(Map<String, Object> criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Luôn lọc các record chưa bị xóa
            predicates.add(builder.equal(root.get("isDeleted"), false));

            if (criteria == null || criteria.isEmpty()) {
                return builder.and(predicates.toArray(new Predicate[0]));
            }

            // Lọc theo tierName (partial, case-insensitive)
            String tierName = (String) criteria.get("tierName");
            if (!ValidationUtils.isNullOrEmpty(tierName)) {
                predicates.add(builder.like(builder.lower(root.get("tierName")), "%" + tierName.toLowerCase() + "%"));
            }

            // Lọc theo tierLevel (exact match)
            String tierLevel = (String) criteria.get("tierLevel");
            if (!ValidationUtils.isNullOrEmpty(tierLevel)) {
                predicates.add(builder.equal(root.get("tierLevel"), Integer.valueOf(tierLevel)));
            }

            // Lọc theo isDefault (exact match)
            String isDefault = (String) criteria.get("isDefault");
            if (!ValidationUtils.isNullOrEmpty(isDefault)) {
                predicates.add(builder.equal(root.get("isDefault"), Boolean.valueOf(isDefault)));
            }

            SpecUtils.addAuditDateFilters(root, builder, predicates, criteria);

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
