package com.talex.server.controllers.subscription;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.entities.creator.RevenueTransaction;
import com.talex.server.services.subscription.SubscriptionRevenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/subscription-revenue")
@RequiredArgsConstructor
@Tag(name = "Subscription Revenue Distribution", description = "API tính toán & phân bổ doanh thu Premium cho Creator")
public class SubscriptionRevenueController {

    private final SubscriptionRevenueService subscriptionRevenueService;

    @PostMapping("/demo-distribute")
    @Operation(
            summary = "Tính thử (Demo) phân bổ doanh thu Premium cho Creator",
            description = "Tính toán chi tiết số tiền phân bổ cho từng Creator theo monthYear nhưng KHÔNG thay đổi cơ sở dữ liệu (isDemo = true)."
    )
    public ResponseEntity<BaseResponse> demoDistribute(
            @RequestParam("monthYear") LocalDate monthYear,
            @RequestParam("isDemo") Boolean isDemo
    ) {
        List<RevenueTransaction> transactions = subscriptionRevenueService.processAndDistributePremiumRevenue(monthYear, isDemo);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Tính toán demo phân bổ doanh thu Premium thành công")
                .data(transactions)
                .build());
    }
}