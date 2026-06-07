package com.talex.server.utils;

import com.talex.server.exceptions.details.ContentModuleException;

import java.text.Normalizer;
import java.util.Locale;

public class SlugUtils {
    public static String normalizeSlug(String slug, String fallback) {
        String value = slug == null || slug.isBlank() ? fallback : slug;
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        if (normalized.isBlank()) {
            throw ContentModuleException.badRequest("Slug cannot be empty");
        }
        return normalized;
    }

}
