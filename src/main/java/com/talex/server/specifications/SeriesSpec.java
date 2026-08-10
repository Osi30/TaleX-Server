package com.talex.server.specifications;

import com.talex.server.dtos.requests.series.SeriesSearchCriteria;
import com.talex.server.entities.series.Series;
import com.talex.server.entities.series.SeriesCategory;
import com.talex.server.entities.series.SeriesTag;
import com.talex.server.utils.ValidationUtils;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class SeriesSpec {

    public static Specification<Series> filterByCriteria(SeriesSearchCriteria criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Chỉ lấy các series chưa bị xóa mềm
            predicates.add(builder.equal(root.get("isDeleted"), false));

            if (criteria == null) {
                return builder.and(predicates.toArray(new Predicate[0]));
            }

            // 2. Lọc theo seriesId
            if (!ValidationUtils.isNullOrEmpty(criteria.getSeriesId())) {
                predicates.add(builder.equal(root.get("seriesId"), criteria.getSeriesId()));
            }

            // 3. Tìm kiếm theo title và description (Bỏ dấu tiếng Việt)
            if (!ValidationUtils.isNullOrEmpty(criteria.getSearch())) {
                String cleanKeyword = stripVietnameseAccents(criteria.getSearch().trim().toLowerCase());
                String likePattern = "%" + cleanKeyword + "%";

                // Sử dụng hàm unaccent() trong DB PostgreSQL để chuyển giá trị cột title và description thành không dấu
                Expression<String> unaccentTitle = builder.function("unaccent", String.class, builder.lower(root.get("title")));
                Expression<String> unaccentDesc = builder.function("unaccent", String.class, builder.lower(root.get("description")));

                predicates.add(builder.or(
                        builder.like(unaccentTitle, likePattern),
                        builder.like(unaccentDesc, likePattern)
                ));
            }

            // 4. Lọc theo contentType (VIDEO, COMIC)
            if (criteria.getContentType() != null) {
                predicates.add(builder.equal(root.get("contentType"), criteria.getContentType()));
            }

            // 5. Lọc theo danh sách ageRating (String list: EVERYONE, TEEN, MATURE,...)
            if (criteria.getAgeRatings() != null && !criteria.getAgeRatings().isEmpty()) {
                predicates.add(root.get("ageRating").in(criteria.getAgeRatings()));
            }

            // 6. Lọc theo status (SeriesStatus single)
            if (criteria.getStatus() != null) {
                predicates.add(builder.equal(root.get("status"), criteria.getStatus()));
            }

            // 7. Lọc theo danh sách categoryId
            if (criteria.getCategoryIds() != null && !criteria.getCategoryIds().isEmpty()) {
                Join<Series, SeriesCategory> categoryJoin = root.join("seriesCategories", JoinType.INNER);
                predicates.add(categoryJoin.get("category").get("categoryId").in(criteria.getCategoryIds()));
            }

            // 8. Lọc theo danh sách tagId
            if (criteria.getTagIds() != null && !criteria.getTagIds().isEmpty()) {
                Join<Series, SeriesTag> tagJoin = root.join("seriesTags", JoinType.INNER);
                predicates.add(tagJoin.get("tag").get("tagId").in(criteria.getTagIds()));
            }

            // Đảm bảo không trùng lặp record khi JOIN với bảng liên kết N-N
            query.distinct(true);

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Hàm hỗ trợ loại bỏ toàn bộ dấu tiếng Việt trên Java side trước khi đưa vào SQL LIKE
     */
    private static String stripVietnameseAccents(String text) {
        if (text == null) return "";
        String nfd = Normalizer.normalize(text, Normalizer.Form.NFD);
        String noMarks = nfd.replaceAll("\\p{InCombiningDiacriticalMarks}", "");
        return noMarks.replace('đ', 'd').replace('Đ', 'D');
    }
}