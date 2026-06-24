package com.talex.server.repositories.interaction;

import com.talex.server.entities.interaction.AccountInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface AccountInteractionRepository extends JpaRepository<AccountInteraction, String> {

    @Modifying
    @Transactional
    @Query(value = "MERGE INTO account_interaction t " +
            "USING (SELECT :accountId AS account_id, :episodeId AS episode_id) s " +
            "ON (t.account_id = s.account_id AND t.episode_id = s.episode_id) " +
            "WHEN MATCHED AND " +
            "  (CASE WHEN :interactionType = 'LIKE' THEN true WHEN :interactionType = 'UNLIKE' THEN false ELSE t.is_like END) = false AND " +
            "  (CASE WHEN :interactionType = 'BOOKMARK' THEN true WHEN :interactionType = 'UNBOOKMARK' THEN false ELSE t.is_bookmark END) = false THEN " +
            "    DELETE " +
            "WHEN MATCHED THEN " +
            "    UPDATE SET " +
            "      is_like = CASE WHEN :interactionType = 'LIKE' THEN true WHEN :interactionType = 'UNLIKE' THEN false ELSE t.is_like END, " +
            "      is_bookmark = CASE WHEN :interactionType = 'BOOKMARK' THEN true WHEN :interactionType = 'UNBOOKMARK' THEN false ELSE t.is_bookmark END, " +
            "      updated_at = NOW() " +
            "WHEN NOT MATCHED AND (:interactionType = 'LIKE' OR :interactionType = 'BOOKMARK') THEN " +
            "    INSERT (id, account_id, episode_id, is_like, is_bookmark, created_at, updated_at) " +
            "    VALUES ( " +
            "      gen_random_uuid(), " +
            "      s.account_id, " +
            "      s.episode_id, " +
            "      CASE WHEN :interactionType = 'LIKE' THEN true ELSE false END, " +
            "      CASE WHEN :interactionType = 'BOOKMARK' THEN true ELSE false END, " +
            "      NOW(), " +
            "      NOW() " +
            "    )",
            nativeQuery = true)
    void upsertOrDeleteInteraction(
            @Param("accountId") UUID accountId,
            @Param("episodeId") String episodeId,
            @Param("interactionType") String interactionType
    );
}