package com.talex.server.services.campaign;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.campaign.response.CampaignWalletBalanceDto;
import com.talex.server.dtos.campaign.response.CampaignWalletTransactionDto;
import com.talex.server.entities.campaign.Campaign;
import com.talex.server.entities.campaign.CampaignWallet;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CampaignWalletService {

    /**
     * Lấy Ví Quảng Cáo của Creator theo accountId, nếu chưa có thì khởi tạo mới.
     */
    CampaignWallet getOrCreateWalletByAccountId(UUID accountId);

    /**
     * Xử lý tính toán tiền thừa và hoàn tiền vào ví khi Campaign bị hủy/xóa.
     */
    void refundCampaign(Campaign campaign);

    BigDecimal getAvailableBalance(UUID accountId);

    CampaignWalletBalanceDto getWalletBalanceDto(UUID accountId);

    void debitWallet(UUID accountId, BigDecimal amount, String description, String orderId);

    void creditWallet(UUID accountId, BigDecimal amount, String description, String orderId);

    List<CampaignWalletTransactionDto> getTransactionsByOrderId(String orderId);

    List<CampaignWalletTransactionDto> getTransactionsByCampaignId(String campaignId);

    BasePageResponse<CampaignWalletTransactionDto> getWalletHistory(UUID accountId, Pageable pageable);
}