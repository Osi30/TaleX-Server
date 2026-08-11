package com.talex.server.services.payment.impls;

import com.talex.server.entities.series.EpisodeUnlockedContent;
import com.talex.server.entities.transaction.Order;
import com.talex.server.services.creator.RevenueTransactionService;
import com.talex.server.services.series.EpisodeUnlockedContentService;
import com.talex.server.services.payment.OrderFulfillmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EpisodeOrderFulfillmentServiceImpl implements OrderFulfillmentService {

    public static final String ITEM_TYPE = "EPISODE";

    private final EpisodeUnlockedContentService episodeUnlockedContentService;
    private final RevenueTransactionService revenueTransactionService;

    @Override
    public String getSupportedItemType() {
        return ITEM_TYPE;
    }

    @Override
    public void fulfill(Order order) {
        List<EpisodeUnlockedContent> unlockedContents = episodeUnlockedContentService.createFromOrder(
                order.getOrderId(), order.getItemId(), ITEM_TYPE, order.getAccount().getAccountId());

        revenueTransactionService.createFromEpisodeOrder(order, unlockedContents);
    }


}
