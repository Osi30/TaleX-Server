package com.talex.server.specifications;

import com.talex.server.entities.creator.CreatorMonthlySettlement;
import com.talex.server.enums.transaction.SettlementStatus;
import com.talex.server.utils.SpecUtils;
import com.talex.server.utils.ValidationUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CreatorMonthlySettlementSpec {

    public static Specification<CreatorMonthlySettlement> filterByCriteria(
            Map<String, Object> criteria,
            SettlementStatus[] statuses
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Lọc theo Mảng Trạng thái Status (Nhiều trạng thái cùng lúc)
            if (statuses != null && statuses.length > 0) {
                predicates.add(root.get("status").in(Arrays.asList(statuses)));
            }

            if (criteria != null && !criteria.isEmpty()) {
                // 2. Lọc theo ID của bảng Settlement
                String settlementId = (String) criteria.get("creatorMonthlySettlementId");
                if (ValidationUtils.isNullOrEmpty(settlementId)) {
                    settlementId = (String) criteria.get("settlementId"); // Hỗ trợ key ngắn hơn
                }
                if (!ValidationUtils.isNullOrEmpty(settlementId)) {
                    predicates.add(builder.equal(root.get("creatorMonthlySettlementId"), settlementId));
                }

                // 3. Lọc theo Tháng quyết toán (Ví dụ: "2026-07")
                String settlementMonth = (String) criteria.get("settlementMonth");
                if (!ValidationUtils.isNullOrEmpty(settlementMonth)) {
                    predicates.add(builder.equal(root.get("settlementMonth"), settlementMonth));
                }

                // 4. Lọc theo Creator ID
                String creatorId = (String) criteria.get("creatorId");
                if (!ValidationUtils.isNullOrEmpty(creatorId)) {
                    predicates.add(builder.equal(root.get("creator").get("creatorId"), creatorId));
                }

                // 5. Lọc theo khoảng số tiền Net Payout (netPayoutFrom -> netPayoutTo)
                String netPayoutFrom = (String) criteria.get("netPayoutFrom");
                if (!ValidationUtils.isNullOrEmpty(netPayoutFrom)) {
                    predicates.add(builder.greaterThanOrEqualTo(root.get("netPayoutAmount"), Double.valueOf(netPayoutFrom)));
                }
                String netPayoutTo = (String) criteria.get("netPayoutTo");
                if (!ValidationUtils.isNullOrEmpty(netPayoutTo)) {
                    predicates.add(builder.lessThanOrEqualTo(root.get("netPayoutAmount"), Double.valueOf(netPayoutTo)));
                }

                // 6. Tích hợp lọc ngày tạo / cập nhật dùng chung từ SpecUtils
                SpecUtils.addAuditDateFilters(root, builder, predicates, criteria);
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}