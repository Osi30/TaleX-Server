package com.talex.server.services.coin;

public interface AdmobVerificationService {

    /**
     * Xac thuc chu ky va xu ly phan thuong tu Google AdMob SSV.
     *
     * @param signature     Chu ky dien tu tu Google.
     * @param keyId         ID cua khoa cong khai dung de xac thuc.
     * @param customData    Du lieu tuy chinh chua accountId va missionCode.
     * @param transactionId ID giao dich duy nhat tu Google, dung de chong trung lap.
     * @param timestamp     Thoi gian giao dich.
     * @param queryString   Toan bo chuoi query parameters goc, dung de verify signature.
     */
    void processAdmobReward(
            String signature,
            String keyId,
            String customData,
            String transactionId,
            String timestamp,
            String queryString);
}
