package com.talex.server.services.creator;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.filters.CreatorSettlementFilterRequestDto;
import com.talex.server.dtos.settlement.request.UpdateSettlementStatusRequestDto;
import com.talex.server.dtos.settlement.response.CreatorSettlementDetailResponseDto;
import com.talex.server.dtos.settlement.response.CreatorSettlementResponseDto;
import com.talex.server.entities.creator.CreatorMonthlySettlement;

import java.util.List;

public interface CreatorSettlementService {

    /**
     * Chạy quyết toán hàng tháng cho tháng vừa qua (M-1)
     */
    List<CreatorMonthlySettlement> processMonthlySettlement(boolean isDemo);

    /**
     * Chạy quyết toán hàng tháng cho một tháng chỉ định (Dùng cho Demo/Testing, ví dụ: "2026-07")
     */
    List<CreatorMonthlySettlement> processMonthlySettlement(boolean isDemo, String targetMonthStr);

    /**
     * Phân trang và tìm kiếm linh hoạt cho Admin
     */
    BasePageResponse<CreatorSettlementResponseDto> filterSettlements(
            CreatorSettlementFilterRequestDto filterRequest
    );

    /**
     * Admin xử lý quyết toán
     */
    CreatorSettlementDetailResponseDto updateSettlementStatus(
            String settlementId,
            UpdateSettlementStatusRequestDto request
    );

    /**
     * Phân trang và tìm kiếm chi tiết
     */
    CreatorSettlementDetailResponseDto getSettlementById(String id);
}