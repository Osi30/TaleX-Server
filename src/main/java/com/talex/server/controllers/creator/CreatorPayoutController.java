package com.talex.server.controllers.creator;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.payout.request.BatchPayoutRequestDto;
import com.talex.server.services.creator.CreatorPayoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/creator-payout")
@RequiredArgsConstructor
@Tag(name = "Creator Payout", description = "API chạy thử và thực thi chuyển tiền chi hộ (Batch Payout) cho Creator")
public class CreatorPayoutController {

    private final CreatorPayoutService creatorPayoutService;

    @PostMapping("/demo-batch-request")
    @Operation(
            summary = "Xem trước (Demo) cấu trúc BatchPayoutRequestDto",
            description = "Truyền month_year (ví dụ: '2026-07') để lấy danh sách các lệnh chi được tạo từ Settlement Status = CALCULATED (isDemo = true)."
    )
    public ResponseEntity<BaseResponse> getDemoBatchPayoutRequest(
            @RequestParam("monthYear") String monthYear,
            @RequestParam("isDemo") Boolean isDemo
    ) {
        BatchPayoutRequestDto requestDto = creatorPayoutService.processMonthlyPayout(monthYear, isDemo);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Tạo demo BatchPayoutRequest thành công")
                .data(requestDto)
                .build());
    }
}