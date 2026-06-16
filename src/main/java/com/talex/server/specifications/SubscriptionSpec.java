package com.talex.server.specifications;

import com.talex.server.dtos.requests.filters.SubscriptionFilterRequestDto;
import com.talex.server.entities.subscription.Subscription;
import com.talex.server.utils.SpecUtils;
import com.talex.server.utils.ValidationUtils;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SubscriptionSpec {

    public static Specification<Subscription> filterByCriteria(SubscriptionFilterRequestDto filterRequest) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicateList = new ArrayList<>();

            predicateList.add(criteriaBuilder.equal(root.get("isDeleted"), false));
            Map<String, Object> criteria = filterRequest.getCriteria();

            String searchKey = (String) criteria.get("searchKey");
            if (!ValidationUtils.isNullOrEmpty(searchKey)) {
                String term = "%" + searchKey.toLowerCase() + "%";
                Predicate tierPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("tier")), term);
                Predicate descriptionPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("description"), "")),
                        term);
                predicateList.add(criteriaBuilder.or(tierPredicate, descriptionPredicate));
            }

            String minPrice = (String) criteria.get("minPrice");
            if (!ValidationUtils.isNullOrEmpty(minPrice)) {
                predicateList.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), Long.parseLong(minPrice)));
            }

            String maxPrice = (String) criteria.get("maxPrice");
            if (!ValidationUtils.isNullOrEmpty(maxPrice)) {
                predicateList.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), Long.parseLong(maxPrice)));
            }

            String minDuration = (String) criteria.get("minDuration");
            if (!ValidationUtils.isNullOrEmpty(minDuration)) {
                predicateList.add(criteriaBuilder.greaterThanOrEqualTo(root.get("duration"), Integer.parseInt(minDuration)));
            }

            String maxDuration = (String) criteria.get("maxDuration");
            if (!ValidationUtils.isNullOrEmpty(maxDuration)) {
                predicateList.add(criteriaBuilder.lessThanOrEqualTo(root.get("duration"), Integer.parseInt(maxDuration)));
            }

            List<String> durationUnits = Arrays.stream(filterRequest.getDurationUnits()).toList();
            if (!durationUnits.isEmpty()) {
                predicateList.add(root.get("durationUnit").in(durationUnits));
            }

            String minTotalPurchases = (String) criteria.get("minTotalPurchases");
            if (!ValidationUtils.isNullOrEmpty(minTotalPurchases)) {
                predicateList.add(criteriaBuilder.greaterThanOrEqualTo(root.get("totalPurchases"), Long.parseLong(minTotalPurchases)));
            }

            String maxTotalPurchases = (String) criteria.get("maxTotalPurchases");
            if (!ValidationUtils.isNullOrEmpty(maxTotalPurchases)) {
                predicateList.add(criteriaBuilder.lessThanOrEqualTo(root.get("totalPurchases"), Long.parseLong(maxTotalPurchases)));
            }

            SpecUtils.addPermissionsSubscriptionFilters(root, criteriaBuilder, predicateList, criteria);

            SpecUtils.addAuditDateFilters(root, criteriaBuilder, predicateList, criteria);

            if (ValidationUtils.isNullOrEmpty(filterRequest.getSortBy()) && !ValidationUtils.isNullOrEmpty(searchKey)) {
                Expression<Object> relevance = criteriaBuilder.selectCase()
                        .when(criteriaBuilder.like(criteriaBuilder.lower(root.get("tier")),
                                "%" + searchKey.toLowerCase() + "%"), 0)
                        .when(criteriaBuilder.like(
                                criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("description"), "")),
                                "%" + searchKey.toLowerCase() + "%"), 1)
                        .otherwise(2);

                query.orderBy(
                        criteriaBuilder.asc(relevance),
                        criteriaBuilder.desc(root.get("createdAt")),
                        criteriaBuilder.desc(root.get("updatedAt")));
            }

            Predicate[] predicates = predicateList.toArray(new Predicate[0]);
            return criteriaBuilder.and(predicates);
        };
    }
}
