package com.talex.server.repositories.interaction;

import com.talex.server.entities.interaction.AccountComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountCommentRepository extends JpaRepository<AccountComment, String> {

    /// Chủ comment update
    @Query("SELECT c " +
            "FROM AccountComment c " +
            "WHERE c.commentId = :commentId " +
            "AND c.account.accountId = :accountId " +
            "AND c.isHidden = false")
    Optional<AccountComment> findByIdAndAccountIdForUpdate(
            @Param("commentId") String commentId,
            @Param("accountId") UUID accountId
    );

    /// Chủ comment xóa
    @Modifying
    @Query("UPDATE AccountComment c " +
            "SET c.isHidden = true, c.content = 'Bình luận đã bị ẩn hoặc xóa' " +
            "WHERE c.commentId = :commentId " +
            "AND c.account.accountId = :accountId " +
            "AND c.isHidden = false")
    int deleteByCommentIdAndAccountId(
            @Param("commentId") String commentId,
            @Param("accountId") UUID accountId
    );

    /// Admin/Staff ẩn comment
    @Modifying
    @Query("UPDATE AccountComment c " +
            "SET c.isHidden = true, c.content = 'Bình luận đã bị ẩn hoặc xóa' " +
            "WHERE c.commentId = :commentId " +
            "AND c.isHidden = false")
    int hideCommentByAdmin(@Param("commentId") String commentId);

    /// Lấy danh sách bình luận GỐC của Tập phim
    @Query("SELECT c FROM AccountComment c " +
            "JOIN FETCH c.account " +
            "WHERE c.episode.episodeId = :episodeId " +
            "AND c.parentComment IS NULL " +
            "AND (c.isHidden = false OR (c.isHidden = true AND SIZE(c.replies) > 0))")
    Slice<AccountComment> findTopLevelComments(
            @Param("episodeId") String episodeId,
            Pageable pageable
    );

    /// Lấy danh sách các bình luận PHẢN HỒI (Replies) của một bình luận cha
    @Query("SELECT c FROM AccountComment c " +
            "JOIN FETCH c.account " +
            "WHERE c.parentComment.commentId = :parentCommentId " +
            "AND (c.isHidden = false OR (c.isHidden = true AND SIZE(c.replies) > 0))")
    Slice<AccountComment> findRepliesByParentId(
            @Param("parentCommentId") String parentCommentId,
            Pageable pageable
    );
}