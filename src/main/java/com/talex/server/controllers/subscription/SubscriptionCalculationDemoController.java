package com.talex.server.controllers.subscription;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.revenue.request.RuleXCalculationRequestDto;
import com.talex.server.dtos.revenue.response.RuleXCalculationResponseDto;
import com.talex.server.entities.subscription.Subscription;
import com.talex.server.entities.subscription.SubscriptionResult;
import com.talex.server.services.subscription.SubscriptionService;
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
    private final SubscriptionService subscriptionService;

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

    @PostMapping("/process-stats")
    @Operation(
            summary = "Kiểm thử tiến trình gom watch session vào subscription stats",
            description = "Mô phỏng lại luồng chạy của Cron job: lấy các watch session chưa quét kể từ lần quét trước và cập nhật vào subscription_stats."
    )
    public ResponseEntity<BaseResponse> processStats() {
        int processedCount = subscriptionStatService.processSubscriptionStats();
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Thu thập và xử lý Watch Sessions thành công")
                .data("Số lượng session đã xử lý: " + processedCount)
                .build());
    }

    @GetMapping("/export-request-data")
    @Operation(
            summary = "Lấy dữ liệu Subscription Stat theo tháng và map thành RuleXCalculationRequestDto",
            description = "Lấy dữ liệu thống kê từ DB theo monthYear và biến đổi thành định dạng request đầu vào cho thuật toán Rule X."
    )
    public ResponseEntity<BaseResponse> exportRequestData(
            @RequestParam("monthYear") String monthYear,
            @RequestParam(value = "subscriptionId") String subscriptionId
    ) {
        Subscription service = subscriptionService.getSubscriptionByIdEntity(subscriptionId);
        RuleXCalculationRequestDto requestDto = subscriptionStatService.getRuleXRequestFromStats(monthYear, service);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Tạo dữ liệu RuleXCalculationRequestDto thành công")
                .data(requestDto)
                .build());
    }

    @PostMapping("/calculate-and-save")
    @Operation(
            summary = "Tính toán Rule X từ DB stats theo tháng & lưu kết quả phân chia doanh thu",
            description = "Trích xuất thống kê từ DB theo monthYear và subscriptionId, tính toán Rule X, lưu dữ liệu phân bổ vào các entity SubscriptionResult và SubscriptionRevenueLog, sau đó trả về kết quả chia tiền."
    )
    public ResponseEntity<BaseResponse> calculateAndSave(
            @RequestParam("monthYear") String monthYear,
            @RequestParam("subscriptionId") String subscriptionId,
            @RequestParam("isDemo") Boolean isDemo
    ) {
        Subscription service = subscriptionService.getSubscriptionByIdEntity(subscriptionId);
        SubscriptionResult response = subscriptionStatService.calculateAndSaveRevenue(monthYear, service, isDemo);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Tính toán và lưu dữ liệu phân chia doanh thu thành công")
                .data(response)
                .build());
    }
}