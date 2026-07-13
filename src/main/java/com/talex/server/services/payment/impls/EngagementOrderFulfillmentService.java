package com.talex.server.services.payment.impls;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.requests.campaign.CampaignRequestDto;
import com.talex.server.entities.transaction.Order;
import com.talex.server.services.campaign.ICampaignService;
import com.talex.server.services.payment.IOrderFulfillmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EngagementOrderFulfillmentService implements IOrderFulfillmentService {

    public static final String ITEM_TYPE = "ENGAGEMENT";

    private final ICampaignService campaignService;
    private final ObjectMapper objectMapper;

    @Override
    public String getSupportedItemType() {
        return ITEM_TYPE;
    }

    @Override
    public void fulfill(Order order) {
        List<String> seriesIds = deserializeSeriesIds(order.getMetadata());

        CampaignRequestDto campaignRequest = CampaignRequestDto.builder()
                .accountId(order.getAccount().getAccountId())
                .orderId(order.getOrderId())
                .engagementServiceId(order.getItemId())
                .seriesIds(seriesIds)
                .build();

        campaignService.createCampaign(campaignRequest);
    }

    private List<String> deserializeSeriesIds(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            throw new IllegalStateException("Order metadata missing seriesIds for engagement fulfillment");
        }
        try {
            return objectMapper.readValue(metadata, new TypeReference<List<String>>() {
            });
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse order metadata for engagement fulfillment", exception);
        }
    }
}
