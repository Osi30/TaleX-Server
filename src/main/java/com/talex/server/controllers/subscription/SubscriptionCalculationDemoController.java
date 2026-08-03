package com.talex.server.controllers.subscription;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.revenue.request.RuleXCalculationRequestDto;
import com.talex.server.dtos.revenue.response.RuleXCalculationResponseDto;
import com.talex.server.services.subscription.SubscriptionStatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscription-calculation-demo")
@RequiredArgsConstructor
@Tag(name = "Subscription Calculation Demo", description = "API demo thuật toán phân chia doanh thu Fraud-Proof (Rule X)")
public class SubscriptionCalculationDemoController {

    private final SubscriptionStatService subscriptionStatService;

    @PostMapping("/calculate-rulex")
    @Operation(
            summary = "Tính toán hằng số Gamma và doanh thu theo Rule X",
            description = "Nhận vào danh sách lượt nghe của người dùng và tính toán chỉ số Gamma cùng doanh thu cho từng nghệ sĩ."
    )
    public ResponseEntity<BaseResponse> calculateRuleX(
            @Valid @RequestBody RuleXCalculationRequestDto request
    ) {
        RuleXCalculationResponseDto response = subscriptionStatService.calculateRuleX(request);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Tính toán chỉ số Gamma và phân chia doanh thu Rule X thành công")
                .data(response)
                .build());
    }
}