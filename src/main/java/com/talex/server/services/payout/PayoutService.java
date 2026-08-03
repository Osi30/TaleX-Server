package com.talex.server.services.payout;

import com.talex.server.dtos.payout.response.BatchPayoutDataResponseDto;
import com.talex.server.dtos.payout.request.BatchPayoutRequestDto;
import com.talex.server.dtos.payout.response.PayoutAccountBalanceResponseDto;

public interface PayoutService {
    /**
     * Tạo và gửi lô lệnh chi hàng loạt tới PayOS
     *
     * @param requestDto Thông tin chi tiết lô lệnh chi
     * @return Dữ liệu kết quả chi hộ từ PayOS
     */
    BatchPayoutDataResponseDto createBatchPayout(BatchPayoutRequestDto requestDto);

    /**
     * Lấy dữ liệu số dư ví chi hộ
     * @return Dữ liệu số dư ví chi hộ từ PayOS
     */
    PayoutAccountBalanceResponseDto getAccountBalance();
}
