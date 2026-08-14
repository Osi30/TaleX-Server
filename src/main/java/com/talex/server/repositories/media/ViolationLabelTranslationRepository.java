package com.talex.server.repositories.media;

import com.talex.server.entities.media.ViolationLabelTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ViolationLabelTranslationRepository extends JpaRepository<ViolationLabelTranslation, UUID> {

    // category là quan hệ @ManyToOne NULLABLE (nhiều bản dịch chưa gán nhóm) — derived query
    // kiểu "OrderByCategory_NameAsc" sẽ sinh INNER JOIN theo mặc định của Spring Data JPA,
    // âm thầm làm rơi mất các dòng category=null khỏi kết quả. Dùng @Query với LEFT JOIN
    // tường minh để giữ đủ toàn bộ dòng dù có/không có nhóm.
    @Query("SELECT t FROM ViolationLabelTranslation t LEFT JOIN t.category c "
            + "WHERE t.isDeleted = false ORDER BY c.name ASC NULLS LAST, t.awsLabel ASC")
    List<ViolationLabelTranslation> findAllByIsDeletedFalseOrderByCategory_NameAscAwsLabelAsc();

    Optional<ViolationLabelTranslation> findByTranslationIdAndIsDeletedFalse(UUID translationId);

    Optional<ViolationLabelTranslation> findByAwsLabelAndIsDeletedFalse(String awsLabel);

    boolean existsByAwsLabelAndIsDeletedFalse(String awsLabel);
}
