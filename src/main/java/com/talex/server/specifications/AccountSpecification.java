package com.talex.server.specifications;

import com.talex.server.entities.Account;
import com.talex.server.enums.AccountStatus;
import com.talex.server.exceptions.codes.AdminAccountErrorCode;
import com.talex.server.exceptions.details.AdminAccountException;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
public class AccountSpecification implements Specification<Account> {

    private String keyword;
    private String roleName;
    private String status;

    @Override
    public Predicate toPredicate(jakarta.persistence.criteria.Root<Account> root,
                                 jakarta.persistence.criteria.CriteriaQuery<?> query,
                                 jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            String term = "%" + keyword.trim().toLowerCase() + "%";
            Predicate emailPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("email"), "")),
                    term);
            Predicate usernamePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("username"), "")),
                    term);
            Predicate fullNamePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("fullName"), "")),
                    term);
            predicates.add(criteriaBuilder.or(emailPredicate, usernamePredicate, fullNamePredicate));
        }

        if (roleName != null && !roleName.isBlank()) {
            predicates.add(criteriaBuilder.equal(
                    criteriaBuilder.upper(root.join("role", JoinType.LEFT).get("code")),
                    roleName.trim().toUpperCase()));
        }

        if (status != null && !status.isBlank()) {
            predicates.add(criteriaBuilder.equal(root.get("status"), parseStatus(status)));
        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }

    private AccountStatus parseStatus(String value) {
        try {
            return AccountStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new AdminAccountException(AdminAccountErrorCode.INVALID_ACCOUNT_FILTER,
                    "Invalid account status filter: " + value);
        }
    }
}
