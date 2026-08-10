package com.talex.server.controllers.creator;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.entities.creator.CreatorMonthlySettlement;
import com.talex.server.services.creator.CreatorSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/creator-settlement")
@RequiredArgsConstructor
@Tag(name = "Creator Settlement", description = "API chạy thử & tính toán quyết toán doanh thu hàng tháng cho Creator")
public class CreatorSettlementController {

    private final CreatorSettlementService creatorSettlementService;

    @PostMapping("/demo-process")
    @Operation(
            summary = "Chạy thử (Demo) quy trình quyết toán hàng tháng cho Creator",
            description = "Tính toán thuế và tổng số tiền Net Payout cho tất cả Creator đủ điều kiện mà KHÔNG lưu CSDL (isDemo = true)."
    )
    public ResponseEntity<BaseResponse> demoProcessSettlement(
            @RequestParam("isDemo") Boolean isDemo
    ) {
        List<CreatorMonthlySettlement> results = creatorSettlementService.processMonthlySettlement(isDemo);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Chạy demo quyết toán hàng tháng thành công")
                .data(results)
                .build());
    }
}