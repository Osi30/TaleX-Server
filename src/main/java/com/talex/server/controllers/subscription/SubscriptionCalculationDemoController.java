package com.talex.server.controllers.subscription;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.revenue.request.RuleXCalculationRequestDto;
import com.talex.server.dtos.revenue.response.RuleXCalculationResponseDto;
import com.talex.server.entities.subscription.SubscriptionResult;
import com.talex.server.services.subscription.SubscriptionStatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/calculate-and-save")
    @Operation(
            summary = "Tính toán Rule X từ DB stats theo tháng & lưu kết quả phân chia doanh thu",
            description = "Trích xuất thống kê từ DB theo monthYear và Order thực tế, tính toán Rule X cho từng nhóm gói."
    )
    public ResponseEntity<BaseResponse> calculateAndSave(
            @RequestParam("monthYear") String monthYear,
            @RequestParam("isDemo") Boolean isDemo
    ) {
        List<SubscriptionResult> response = subscriptionStatService.calculateAndSaveRevenue(monthYear, isDemo);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Tính toán và lưu dữ liệu phân chia doanh thu thành công")
                .data(response)
                .build());
    }

    @GetMapping("/export-request-data")
    @Operation(
            summary = "Lấy dữ liệu Subscription Stat theo tháng và map thành danh sách RuleXCalculationRequestDto",
            description = "Lấy dữ liệu thống kê từ DB theo monthYear, gom nhóm theo số tiền (total_amount - vat_amount) và thời hạn sub, trả về danh sách request đầu vào cho Rule X."
    )
    public ResponseEntity<BaseResponse> exportRequestData(
            @RequestParam("monthYear") String monthYear
    ) {
        List<RuleXCalculationRequestDto> requestDTOs = subscriptionStatService.getRuleXRequestFromStats(monthYear);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Tạo danh sách RuleXCalculationRequestDto theo nhóm đơn hàng thành công")
                .data(requestDTOs)
                .build());
    }
}