package com.talex.server.repositories.interaction;

import com.talex.server.entities.interaction.AccountComment;
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
    @Query("DELETE FROM AccountComment c " +
            "WHERE c.commentId = :commentId " +
            "AND c.account.accountId = :accountId"
    )
    int deleteByCommentIdAndAccountId(
            @Param("commentId") String commentId,
            @Param("accountId") UUID accountId
    );

    /// Admin/Staff ẩn comment
    @Modifying
    @Query("UPDATE AccountComment c " +
            "SET c.isHidden = :isHide " +
            "WHERE c.commentId = :commentId " +
            "AND c.isHidden != :isHide")
    int hideCommentByAdmin(
            @Param("commentId") String commentId,
            @Param("isHide") boolean isHide
    );
}