package com.talex.server.services.series;

import com.talex.server.entities.series.EpisodeUnlockedContent;
import java.util.List;
import java.util.UUID;

public interface EpisodeUnlockedContentService {
    /**
     * Unlocks content for a user based on an order.
     * @param orderId The ID of the mock/real order
     * @param itemId The ID of the item being purchased (Episode or Combo)
     * @param itemType The type of item ("EPISODE" or "COMBO")
     * @param accountId The UUID of the account purchasing the item
     * @return List of newly unlocked content records
     */
    List<EpisodeUnlockedContent> createFromOrder(String orderId, String itemId, String itemType, UUID accountId);
}
