package com.talex.server.dtos.requests.coin;

import org.springframework.util.MultiValueMap;

public record AdmobSsvCallbackRequest(
        String adNetwork,
        String adUnit,
        String rewardAmount,
        String rewardItem,
        String customData,
        String signature,
        String keyId,
        String transactionId,
        String timestamp,
        String queryString,
        MultiValueMap<String, String> queryParams) {
}
