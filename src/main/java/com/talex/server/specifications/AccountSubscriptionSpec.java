package com.talex.server.specifications;

import com.talex.server.dtos.requests.filters.AccountSubscriptionFilterRequestDto;
import com.talex.server.entities.AccountSubscription;
import com.talex.server.enums.AccountSubscriptionStatus;
import com.talex.server.utils.SpecUtils;
import com.talex.server.utils.ValidationUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AccountSubscriptionSpec {

    public static Specification<AccountSubscription> filterByCriteria(
            AccountSubscriptionFilterRequestDto filterRequest,
            AccountSubscriptionStatus[] statuses) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicateList = new ArrayList<>();

            if (statuses != null && statuses.length > 0) {
                predicateList.add(root.get("status").in((Object[]) statuses));
            }

            Map<String, Object> criteria = filterRequest.getCriteria();
            if (criteria != null) {
                String startTimeFrom = (String) criteria.get("startTimeFrom");
                if (!ValidationUtils.isNullOrEmpty(startTimeFrom)) {
                    predicateList.add(criteriaBuilder.greaterThanOrEqualTo(
                            root.get("startTime"), LocalDateTime.parse(startTimeFrom)));
                }

                String startTimeTo = (String) criteria.get("startTimeTo");
                if (!ValidationUtils.isNullOrEmpty(startTimeTo)) {
                    predicateList.add(criteriaBuilder.lessThanOrEqualTo(
                            root.get("startTime"), LocalDateTime.parse(startTimeTo)));
                }

                String endTimeFrom = (String) criteria.get("endTimeFrom");
                if (!ValidationUtils.isNullOrEmpty(endTimeFrom)) {
                    predicateList.add(criteriaBuilder.greaterThanOrEqualTo(
                            root.get("endTime"), LocalDateTime.parse(endTimeFrom)));
                }

                String endTimeTo = (String) criteria.get("endTimeTo");
                if (!ValidationUtils.isNullOrEmpty(endTimeTo)) {
                    predicateList.add(criteriaBuilder.lessThanOrEqualTo(
                            root.get("endTime"), LocalDateTime.parse(endTimeTo)));
                }

                String accountId = (String) criteria.get("accountId");
                if (!ValidationUtils.isNullOrEmpty(accountId)) {
                    try {
                        predicateList.add(criteriaBuilder.equal(root.get("account").get("accountId"), UUID.fromString(accountId)));
                    } catch (IllegalArgumentException ignored) {
                        predicateList.add(criteriaBuilder.equal(root.get("account").get("accountId"), accountId));
                    }
                }

                String subscriptionId = (String) criteria.get("subscriptionId");
                if (!ValidationUtils.isNullOrEmpty(subscriptionId)) {
                    predicateList.add(criteriaBuilder.equal(root.get("subscription").get("subscriptionId"), subscriptionId));
                }

                SpecUtils.addPermissionsSubscriptionFilters(root, criteriaBuilder, predicateList, criteria);

                SpecUtils.addAuditDateFilters(root, criteriaBuilder, predicateList, criteria);
            }

            Predicate[] predicates = predicateList.toArray(new Predicate[0]);
            return criteriaBuilder.and(predicates);
        };
    }
}
