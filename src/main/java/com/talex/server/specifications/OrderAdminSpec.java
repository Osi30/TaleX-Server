package com.talex.server.specifications;

import com.talex.server.entities.auth.Account;
import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.utils.ValidationUtils;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin order search. Previously a static JPQL query with a repeated
 * ":keyword IS NULL OR LOWER(...)" pattern — Hibernate 7.2 picked the wrong
 * JDBC bind type for the reused ":keyword" parameter (bound as bytea instead
 * of text), so LOWER() failed at runtime with "function lower(bytea) does
 * not exist" (found 2026-08-24, prod /api/v1/admin/orders 500). Criteria API
 * predicates avoid this — each filter is only added when the Java value is
 * non-null, so there is no reused "param IS NULL OR ..." pattern for
 * Hibernate to misinfer a type for.
 */
public class OrderAdminSpec {

    public static Specification<Order> searchForAdmin(
            OrderStatus status, String itemType, LocalDateTime startDate, LocalDateTime endDate, String keyword) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("account", JoinType.LEFT);
            }
            Join<Order, Account> account = root.join("account", JoinType.LEFT);

            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (!ValidationUtils.isNullOrEmpty(itemType)) {
                predicates.add(builder.equal(root.get("itemType"), itemType));
            }
            if (startDate != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }
            if (!ValidationUtils.isNullOrEmpty(keyword)) {
                String trimmedKeyword = keyword.trim().toLowerCase();
                String pattern = "%" + trimmedKeyword + "%";
                // fullName can hold Vietnamese diacritics (e.g. buyer's real name "Nguyễn Văn A"),
                // so match it accent-insensitively via Postgres unaccent() — same approach as
                // SeriesSpec — while paymentCode/orderId/username/email stay plain LOWER() since
                // those are always ASCII identifiers.
                String unaccentPattern = "%" + ValidationUtils.stripVietnameseAccents(trimmedKeyword) + "%";
                Expression<String> unaccentFullName =
                        builder.function("unaccent", String.class, builder.lower(account.get("fullName")));
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("paymentCode")), pattern),
                        builder.like(builder.lower(root.get("orderId")), pattern),
                        builder.like(builder.lower(account.get("username")), pattern),
                        builder.like(builder.lower(account.get("email")), pattern),
                        builder.like(unaccentFullName, unaccentPattern)
                ));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
