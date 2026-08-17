package com.talex.server.utils;

import java.text.Normalizer;

public class ValidationUtils {
    public static boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    // Loại bỏ dấu tiếng Việt trên Java side trước khi đưa vào SQL LIKE — dùng cùng với hàm
    // unaccent() phía Postgres trên cột đang so khớp (xem SeriesSpec, MediaServiceImpl) để
    // search không phân biệt có/không dấu. Giữ logic y hệt SeriesSpec.stripVietnameseAccents
    // (đã verify đúng ở luồng search Series công khai) — tách ra đây để dùng chung, tránh
    // lặp code khi thêm accent-insensitive search cho các module khác.
    public static String stripVietnameseAccents(String text) {
        if (text == null) return "";
        String nfd = Normalizer.normalize(text, Normalizer.Form.NFD);
        String noMarks = nfd.replaceAll("\\p{InCombiningDiacriticalMarks}", "");
        return noMarks.replace('đ', 'd').replace('Đ', 'D');
    }
}
