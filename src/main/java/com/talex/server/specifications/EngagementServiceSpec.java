package com.talex.server.specifications;

import com.talex.server.entities.campaign.EngagementService;
import com.talex.server.enums.engagement.EngagementTarget;
import com.talex.server.enums.engagement.EngagementType;
import com.talex.server.utils.SpecUtils;
import com.talex.server.utils.ValidationUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EngagementServiceSpec {

    public static Specification<EngagementService> filterByCriteria(
            Map<String, Object> criteria,
            EngagementType[] types,
            EngagementTarget[] targets
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(builder.equal(root.get("isDeleted"), false));

            if (criteria != null && !criteria.isEmpty()) {
                // 1. Search động theo searchKey (OR giữa name và description)
                String searchKey = (String) criteria.get("searchKey");
                if (!ValidationUtils.isNullOrEmpty(searchKey)) {
                    String likePattern = "%" + searchKey + "%";
                    predicates.add(builder.or(
                            builder.like(root.get("name"), likePattern),
                            builder.like(root.get("description"), likePattern)
                    ));
                }

                // 2. Lọc theo trạng thái isActive
                String isActive = (String) criteria.get("isActive");
                if (!ValidationUtils.isNullOrEmpty(isActive)) {
                    predicates.add(builder.equal(root.get("isActive"), Boolean.valueOf(isActive)));
                }

                // 3. Lọc theo khoảng giá price (priceFrom -> priceTo)
                String priceFrom = (String) criteria.get("priceFrom");
                if (!ValidationUtils.isNullOrEmpty(priceFrom)) {
                    predicates.add(builder.greaterThanOrEqualTo(root.get("price"), Long.valueOf(priceFrom)));
                }
                String priceTo = (String) criteria.get("priceTo");
                if (!ValidationUtils.isNullOrEmpty(priceTo)) {
                    predicates.add(builder.lessThanOrEqualTo(root.get("price"), Long.valueOf(priceTo)));
                }

                // 4. Lọc theo khoảng targetValue (targetValueFrom -> targetValueTo)
                String targetValueFrom = (String) criteria.get("targetValueFrom");
                if (!ValidationUtils.isNullOrEmpty(targetValueFrom)) {
                    predicates.add(builder.greaterThanOrEqualTo(root.get("targetValue"), Long.valueOf(targetValueFrom)));
                }
                String targetValueTo = (String) criteria.get("targetValueTo");
                if (!ValidationUtils.isNullOrEmpty(targetValueTo)) {
                    predicates.add(builder.lessThanOrEqualTo(root.get("targetValue"), Long.valueOf(targetValueTo)));
                }

                // 5. Tích hợp lọc khoảng thời gian createdAt và updatedAt từ bộ SpecUtils chung
                SpecUtils.addAuditDateFilters(root, builder, predicates, criteria);
            }

            // 6. Lọc IN theo mảng loại dịch vụ tương tác (EngagementType)
            if (types != null && types.length > 0) {
                predicates.add(root.get("engagementType").in((Object[]) types));
            }

            // 7. Lọc IN theo mảng đối tượng tương tác (EngagementTarget)
            if (targets != null && targets.length > 0) {
                predicates.add(root.get("engagementTarget").in((Object[]) targets));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}