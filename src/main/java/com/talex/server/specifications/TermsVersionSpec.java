package com.talex.server.specifications;

import com.talex.server.entities.TermsVersion;
import com.talex.server.enums.TermsType;
import com.talex.server.utils.SpecUtils;
import com.talex.server.utils.ValidationUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TermsVersionSpec {

    public static Specification<TermsVersion> filterByCriteria(
            Map<String, Object> criteria,
            TermsType[] termsTypes
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria == null || criteria.isEmpty()) {
                return builder.and(predicates.toArray(new Predicate[0]));
            }

            String version = (String) criteria.get("version");
            if (!ValidationUtils.isNullOrEmpty(version)) {
                predicates.add(builder.like(root.get("version"), "%" + version + "%"));
            }

            String title = (String) criteria.get("title");
            if (!ValidationUtils.isNullOrEmpty(title)) {
                predicates.add(builder.like(root.get("title"), "%" + title + "%"));
            }

            String content = (String) criteria.get("content");
            if (!ValidationUtils.isNullOrEmpty(content)) {
                predicates.add(builder.like(root.get("content"), "%" + content + "%"));
            }

            String isActive = (String) criteria.get("isActive");
            if (!ValidationUtils.isNullOrEmpty(isActive)) {
                predicates.add(builder.equal(root.get("isActive"), Boolean.valueOf(isActive)));
            }

            if (termsTypes != null && termsTypes.length > 0) {
                predicates.add(root.get("type").in((Object[]) termsTypes));
            }

            SpecUtils.addAuditDateFilters(root, builder, predicates, criteria);

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
