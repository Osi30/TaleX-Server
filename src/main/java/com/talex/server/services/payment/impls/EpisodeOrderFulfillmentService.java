package com.talex.server.services.payment.impls;

import com.talex.server.entities.transaction.Order;
import com.talex.server.services.series.EpisodeUnlockedContentService;
import com.talex.server.services.payment.IOrderFulfillmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EpisodeOrderFulfillmentService implements IOrderFulfillmentService {

    public static final String ITEM_TYPE = "EPISODE";

    private final EpisodeUnlockedContentService episodeUnlockedContentService;

    @Override
    public String getSupportedItemType() {
        return ITEM_TYPE;
    }

    @Override
    public void fulfill(Order order) {
        episodeUnlockedContentService.createFromOrder(
                order.getOrderId(), order.getItemId(), ITEM_TYPE, order.getAccount().getAccountId());
    }
}
