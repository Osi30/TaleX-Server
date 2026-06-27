package com.talex.server.specifications;

import com.talex.server.entities.campaign.Campaign;
import com.talex.server.enums.engagement.CampaignStatus;
import com.talex.server.enums.engagement.EngagementTarget;
import com.talex.server.utils.SpecUtils;
import com.talex.server.utils.ValidationUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CampaignSpec {

    public static Specification<Campaign> filterByCriteria(
            Map<String, Object> criteria,
            EngagementTarget[] targets,
            CampaignStatus[] statuses
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria != null && !criteria.isEmpty()) {
                // 1. Lọc theo khoảng startAt (startAtFrom -> startAtTo)
                String startAtFrom = (String) criteria.get("startAtFrom");
                if (!ValidationUtils.isNullOrEmpty(startAtFrom)) {
                    predicates.add(builder.greaterThanOrEqualTo(root.get("startAt"), LocalDateTime.parse(startAtFrom)));
                }
                String startAtTo = (String) criteria.get("startAtTo");
                if (!ValidationUtils.isNullOrEmpty(startAtTo)) {
                    predicates.add(builder.lessThanOrEqualTo(root.get("startAt"), LocalDateTime.parse(startAtTo)));
                }

                // 2. Lọc theo khoảng endAt (endAtFrom -> endAtTo)
                String endAtFrom = (String) criteria.get("endAtFrom");
                if (!ValidationUtils.isNullOrEmpty(endAtFrom)) {
                    predicates.add(builder.greaterThanOrEqualTo(root.get("endAt"), LocalDateTime.parse(endAtFrom)));
                }
                String endAtTo = (String) criteria.get("endAtTo");
                if (!ValidationUtils.isNullOrEmpty(endAtTo)) {
                    predicates.add(builder.lessThanOrEqualTo(root.get("endAt"), LocalDateTime.parse(endAtTo)));
                }

                // 3. Lọc theo khoảng mục tiêu targetValue
                SpecUtils.addTargetValueFilters(root, builder, predicates, criteria);

                // 4. Lọc theo khoảng giá trị tích lũy hiện tại currentValue
                String currentValueFrom = (String) criteria.get("currentValueFrom");
                if (!ValidationUtils.isNullOrEmpty(currentValueFrom)) {
                    predicates.add(builder.greaterThanOrEqualTo(root.get("currentValue"), Long.valueOf(currentValueFrom)));
                }
                String currentValueTo = (String) criteria.get("currentValueTo");
                if (!ValidationUtils.isNullOrEmpty(currentValueTo)) {
                    predicates.add(builder.lessThanOrEqualTo(root.get("currentValue"), Long.valueOf(currentValueTo)));
                }

                // 5. Lọc theo episodeId (Khóa ngoại)
                String episodeId = (String) criteria.get("episodeId");
                if (!ValidationUtils.isNullOrEmpty(episodeId)) {
                    predicates.add(builder.equal(root.get("episode").get("episodeId"), episodeId));
                }

                // 6. Lọc theo engagementServiceId (Khóa ngoại)
                String engagementServiceId = (String) criteria.get("engagementServiceId");
                if (!ValidationUtils.isNullOrEmpty(engagementServiceId)) {
                    predicates.add(builder.equal(root.get("engagementService").get("engagementServiceId"), engagementServiceId));
                }

                // 7. Sử dụng cấu hình có sẵn từ SpecUtils để tự động lọc khoảng thời gian khởi tạo & cập nhật nhật ký audit
                SpecUtils.addAuditDateFilters(root, builder, predicates, criteria);
            }

            // 8. Toán tử tìm kiếm Or theo danh sách EngagementTarget (sử dụng mệnh đề IN)
            if (targets != null && targets.length > 0) {
                predicates.add(root.get("engagementTarget").in((Object[]) targets));
            }

            // 9. Toán tử tìm kiếm Or theo danh sách CampaignStatus (sử dụng mệnh đề IN)
            if (statuses != null && statuses.length > 0) {
                predicates.add(root.get("status").in((Object[]) statuses));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}