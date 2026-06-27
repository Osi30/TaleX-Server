package com.talex.server.specifications;

import com.talex.server.dtos.requests.filters.CreatorFilterRequestDto;
import com.talex.server.entities.creator.Creator;
import com.talex.server.utils.ValidationUtils;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CreatorSpec {

    public static Specification<Creator> filterByCriteria(CreatorFilterRequestDto filterRequest) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicateList = new ArrayList<>();

            String searchKey = filterRequest.getSearchKey();
            if (!ValidationUtils.isNullOrEmpty(searchKey)) {
                String term = "%" + searchKey.toLowerCase() + "%";
                Predicate nicknamePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("nickname")), term);
                Predicate bioPredicate = criteriaBuilder
                        .like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("bio"), "")), term);
                predicateList.add(criteriaBuilder.or(nicknamePredicate, bioPredicate));
            }

            String createdAtFrom = filterRequest.getCreatedAtFrom();
            if (!ValidationUtils.isNullOrEmpty(createdAtFrom)) {
                predicateList.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"),
                        LocalDateTime.parse(createdAtFrom)));
            }

            String createdAtTo = filterRequest.getCreatedAtTo();
            if (!ValidationUtils.isNullOrEmpty(createdAtTo)) {
                predicateList.add(
                        criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), LocalDateTime.parse(createdAtTo)));
            }

            String updatedAtFrom = filterRequest.getUpdatedAtFrom();
            if (!ValidationUtils.isNullOrEmpty(updatedAtFrom)) {
                predicateList.add(criteriaBuilder.greaterThanOrEqualTo(root.get("updatedAt"),
                        LocalDateTime.parse(updatedAtFrom)));
            }

            String updatedAtTo = filterRequest.getUpdatedAtTo();
            if (!ValidationUtils.isNullOrEmpty(updatedAtTo)) {
                predicateList.add(
                        criteriaBuilder.lessThanOrEqualTo(root.get("updatedAt"), LocalDateTime.parse(updatedAtTo)));
            }

            if (ValidationUtils.isNullOrEmpty(filterRequest.getSortBy()) && !ValidationUtils.isNullOrEmpty(searchKey)) {
                Expression<Object> relevance = criteriaBuilder.selectCase()
                        .when(criteriaBuilder.like(criteriaBuilder.lower(root.get("nickname")),
                                "%" + searchKey.toLowerCase() + "%"), 0)
                        .when(criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("bio"), "")),
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
