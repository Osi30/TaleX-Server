package com.talex.server.services.campaign;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.payout.request.PayoutRequestProcessDto;
import com.talex.server.dtos.payout.response.PayoutRequestResponseDto;

import java.util.Map;
import java.util.UUID;

public interface PayoutRequestService {

    // Người dùng tạo yêu cầu rút tiền
    PayoutRequestResponseDto createPayoutRequest(UUID accountId);

    // Admin xem danh sách có phân trang và lọc
    BasePageResponse<PayoutRequestResponseDto> getPayoutRequests(Map<String, Object> criteria, Integer page, Integer pageSize);

    // Admin Duyệt / Từ chối yêu cầu
    PayoutRequestResponseDto processPayoutRequest(String payoutRequestId, PayoutRequestProcessDto dto);

    // Gọi PayoutService thực hiện chuyển tiền
    PayoutRequestResponseDto executePayout(String payoutRequestId);
}