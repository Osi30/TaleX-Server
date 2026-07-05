package com.talex.server.specifications;

import com.talex.server.entities.creator.CreatorIdentity;
import com.talex.server.enums.creator.CreatorIdentityStatus;
import com.talex.server.utils.SpecUtils;
import com.talex.server.utils.ValidationUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreatorIdentitySpec {
    public static Specification<CreatorIdentity> filterByCriteria(
            Map<String, Object> criteria,
            CreatorIdentityStatus[] statuses
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria != null && !criteria.isEmpty()) {
                // 1. Lọc theo idNumber
                String idNumber = (String) criteria.get("idNumber");
                if (!ValidationUtils.isNullOrEmpty(idNumber)) {
                    predicates.add(builder.like(root.get("idNumber"), "%" + idNumber + "%"));
                }

                // 2. Lọc theo fullName
                String fullName = (String) criteria.get("fullName");
                if (!ValidationUtils.isNullOrEmpty(fullName)) {
                    predicates.add(builder.like(root.get("fullName"), "%" + fullName + "%"));
                }

                // 3. Lọc theo ngày sinh dob (Dạng chuỗi YYYY-MM-DD)
                String dob = (String) criteria.get("dob");
                if (!ValidationUtils.isNullOrEmpty(dob)) {
                    predicates.add(builder.equal(root.get("dob"), LocalDate.parse(dob)));
                }

                // 4. Lọc theo sex (Giới tính)
                String sex = (String) criteria.get("sex");
                if (!ValidationUtils.isNullOrEmpty(sex)) {
                    predicates.add(builder.equal(root.get("sex"), sex));
                }

                // 5. Lọc theo địa chỉ address
                String address = (String) criteria.get("address");
                if (!ValidationUtils.isNullOrEmpty(address)) {
                    predicates.add(builder.like(root.get("address"), "%" + address + "%"));
                }

                // 6. Lọc theo ngày hết hạn doe (Dạng chuỗi YYYY-MM-DD)
                String doe = (String) criteria.get("doe");
                if (!ValidationUtils.isNullOrEmpty(doe)) {
                    predicates.add(builder.equal(root.get("doe"), LocalDate.parse(doe)));
                }

                // 7. Lọc theo khoảng thời gian verifiedAt (verifiedAtFrom -> verifiedAtTo)
                String verifiedAtFrom = (String) criteria.get("verifiedAtFrom");
                if (!ValidationUtils.isNullOrEmpty(verifiedAtFrom)) {
                    predicates.add(builder.greaterThanOrEqualTo(root.get("verifiedAt"), LocalDateTime.parse(verifiedAtFrom)));
                }
                String verifiedAtTo = (String) criteria.get("verifiedAtTo");
                if (!ValidationUtils.isNullOrEmpty(verifiedAtTo)) {
                    predicates.add(builder.lessThanOrEqualTo(root.get("verifiedAt"), LocalDateTime.parse(verifiedAtTo)));
                }

                // 8. Lọc theo ghi chú verifiedNote
                String verifiedNote = (String) criteria.get("verifiedNote");
                if (!ValidationUtils.isNullOrEmpty(verifiedNote)) {
                    predicates.add(builder.like(root.get("verifiedNote"), "%" + verifiedNote + "%"));
                }

                // 9. Lọc theo mã số thuế taxId
                String taxId = (String) criteria.get("taxId");
                if (!ValidationUtils.isNullOrEmpty(taxId)) {
                    predicates.add(builder.like(root.get("taxId"), "%" + taxId + "%"));
                }

                // 10. Tự động thêm bộ lọc khoảng thời gian khởi tạo & cập nhật bằng SpecUtils
                SpecUtils.addAuditDateFilters(root, builder, predicates, criteria);
            }

            // 11. Tìm kiếm OR cho danh sách status truyền vào (sử dụng mệnh đề IN)
            if (statuses != null && statuses.length > 0) {
                predicates.add(root.get("status").in((Object[]) statuses));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
