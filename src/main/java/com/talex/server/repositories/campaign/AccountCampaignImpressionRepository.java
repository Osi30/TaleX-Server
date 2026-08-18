package com.talex.server.repositories.campaign;

import com.talex.server.entities.campaign.AccountCampaignImpression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface AccountCampaignImpressionRepository extends JpaRepository<AccountCampaignImpression, String> {

    /**
     * Chèn các bản ghi Account Campaign Impression nếu chưa tồn tại.
     * Trả về danh sách các campaign_series_id THỰC SỰ ĐƯỢC CỘNG MỚI (chưa từng ghi nhận cho account này).
     */
    @Modifying
    @Query(value = """
        INSERT INTO account_campaign_impressions (id, account_id, campaign_series_id, created_at)
        SELECT gen_random_uuid(), :accountId, cs.campaign_series_id, NOW()
        FROM campaign_series cs
        WHERE cs.series_id IN (:seriesIds)
          AND cs.status = 'RUNNING'
        ON CONFLICT (account_id, campaign_series_id) DO NOTHING
        RETURNING campaign_series_id
        """, nativeQuery = true)
    List<String> insertIfNotExistsAndGetInsertedIds(
            @Param("accountId") UUID accountId,
            @Param("seriesIds") Collection<String> seriesIds
    );
}