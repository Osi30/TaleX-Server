package com.talex.server.repositories.interaction;

import com.talex.server.dtos.interaction.AccountFollowInfoDto;
import com.talex.server.entities.interaction.AccountFollow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccountFollowRepository extends JpaRepository<AccountFollow, String> {
    @Modifying
    @Query("DELETE FROM AccountFollow af WHERE af.follower.accountId = :followerId AND af.followed.accountId = :followedId")
    int deleteByFollowerIdAndFollowedId(@Param("followerId") UUID followerId, @Param("followedId") UUID followedId);

    /**
     * Lấy danh sách Followers (Những người đang theo dõi tài khoản này) có trạng thái ACTIVE
     */
    @Query("SELECT new com.talex.server.dtos.interaction.AccountFollowInfoDto(af.follower.accountId, af.follower.username, af.follower.avatarUrl, af.createdAt) " +
            "FROM AccountFollow af " +
            "WHERE af.followed.accountId = :accountId " +
            "AND af.follower.status = com.talex.server.enums.AccountStatus.ACTIVE " +
            "ORDER BY af.createdAt DESC, af.id DESC")
    Slice<AccountFollowInfoDto> findFollowersByAccountId(@Param("accountId") UUID accountId, Pageable pageable);

    /**
     * Lấy danh sách Followed (Những tài khoản mà người này đang theo dõi) có trạng thái ACTIVE
     */
    @Query("SELECT new com.talex.server.dtos.interaction.AccountFollowInfoDto(af.followed.accountId, af.followed.username, af.followed.avatarUrl, af.createdAt) " +
            "FROM AccountFollow af " +
            "WHERE af.follower.accountId = :accountId " +
            "AND af.followed.status = com.talex.server.enums.AccountStatus.ACTIVE " +
            "ORDER BY af.createdAt DESC, af.id DESC")
    Slice<AccountFollowInfoDto> findFollowedByAccountId(@Param("accountId") UUID accountId, Pageable pageable);
}