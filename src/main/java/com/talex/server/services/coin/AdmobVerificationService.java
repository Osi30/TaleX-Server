package com.talex.server.services.coin;

import com.talex.server.dtos.requests.coin.AdmobSsvCallbackRequest;

public interface AdmobVerificationService {

    /**
     * Xac thuc chu ky va xu ly phan thuong tu Google AdMob SSV.
     *
     * @param callback Query parameters goc tu Google, dung de verify signature va xu ly reward.
     */
    void processAdmobReward(AdmobSsvCallbackRequest callback);
}
