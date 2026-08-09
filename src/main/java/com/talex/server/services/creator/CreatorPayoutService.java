package com.talex.server.services.creator;

import com.talex.server.dtos.payout.request.BatchPayoutRequestDto;

public interface CreatorPayoutService {
    /**
     * Tạo và (nếu không phải Demo) thực hiện chuyển tiền chi hộ hàng loạt cho Creator
     *
     * @param monthYear Tháng quyết toán (Dạng: YYYY-MM)
     * @param isDemo    Nếu true: Chỉ trả về BatchPayoutRequestDto để kiểm tra request.
     *                  Nếu false: Gọi PayoutService.createBatchPayout() để chuyển tiền thật.
     * @return BatchPayoutRequestDto vừa được tổng hợp
     */
    BatchPayoutRequestDto processMonthlyPayout(String monthYear, boolean isDemo);
}
