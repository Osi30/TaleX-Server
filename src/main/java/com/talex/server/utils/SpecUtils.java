package com.talex.server.utils;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class SpecUtils {
    public static void addAuditDateFilters(
            Root<?> root,
            CriteriaBuilder builder,
            List<Predicate> predicates,
            Map<String, Object> criteria) {

        if (criteria == null || criteria.isEmpty()) {
            return;
        }

        // Filter cho createdAt
        String createdAtFrom = (String) criteria.get("createdAtFrom");
        if (!ValidationUtils.isNullOrEmpty(createdAtFrom)) {
            predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), LocalDateTime.parse(createdAtFrom)));
        }

        String createdAtTo = (String) criteria.get("createdAtTo");
        if (!ValidationUtils.isNullOrEmpty(createdAtTo)) {
            predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), LocalDateTime.parse(createdAtTo)));
        }

        // Filter cho updatedAt
        String updatedAtFrom = (String) criteria.get("updatedAtFrom");
        if (!ValidationUtils.isNullOrEmpty(updatedAtFrom)) {
            predicates.add(builder.greaterThanOrEqualTo(root.get("updatedAt"), LocalDateTime.parse(updatedAtFrom)));
        }

        String updatedAtTo = (String) criteria.get("updatedAtTo");
        if (!ValidationUtils.isNullOrEmpty(updatedAtTo)) {
            predicates.add(builder.lessThanOrEqualTo(root.get("updatedAt"), LocalDateTime.parse(updatedAtTo)));
        }
    }

    public static void addPermissionsSubscriptionFilters(
            Root<?> root,
            CriteriaBuilder builder,
            List<Predicate> predicates,
            Map<String, Object> criteria) {

        if (criteria == null || criteria.isEmpty()) {
            return;
        }

        String isAdBlocked = (String) criteria.get("isAdBlocked");
        if (!ValidationUtils.isNullOrEmpty(isAdBlocked)) {
            predicates.add(builder.equal(root.get("isAdBlocked"), Boolean.parseBoolean(isAdBlocked)));
        }

        String isMovieUnlocked = (String) criteria.get("isMovieUnlocked");
        if (!ValidationUtils.isNullOrEmpty(isMovieUnlocked)) {
            predicates.add(builder.equal(root.get("isMovieUnlocked"), Boolean.parseBoolean(isMovieUnlocked)));
        }

        String isStoryUnlocked = (String) criteria.get("isStoryUnlocked");
        if (!ValidationUtils.isNullOrEmpty(isStoryUnlocked)) {
            predicates.add(builder.equal(root.get("isStoryUnlocked"), Boolean.parseBoolean(isStoryUnlocked)));
        }
    }
}
