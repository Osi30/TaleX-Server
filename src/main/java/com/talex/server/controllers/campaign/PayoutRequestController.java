package com.talex.server.controllers.campaign;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.payout.request.PayoutRequestProcessDto;
import com.talex.server.dtos.payout.response.PayoutRequestResponseDto;
import com.talex.server.services.campaign.PayoutRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payout-requests")
@RequiredArgsConstructor
@Tag(name = "Payout Request", description = "API quản lý yêu cầu rút tiền từ Campaign Wallet")
public class PayoutRequestController {

    private final PayoutRequestService payoutRequestService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Gửi yêu cầu rút tiền",
            description = "Yêu cầu rút toàn bộ số dư trong Ví Campaign Wallet. Yêu cầu số dư khả dụng >= 2.000 VNĐ và đã đăng ký Payment Profile chính."
    )
    public ResponseEntity<BaseResponse> createPayoutRequest(@CurrentAccountId UUID accountId) {
        PayoutRequestResponseDto response = payoutRequestService.createPayoutRequest(accountId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Tạo yêu cầu rút tiền thành công")
                .data(response)
                .build());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy danh sách yêu cầu rút tiền (Admin)",
            description = "Lọc danh sách phân trang theo status, accountId, khoảng ngày tạo,...")
    public ResponseEntity<BaseResponse> getPayoutRequests(
            @RequestParam Map<String, Object> criteria,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        BasePageResponse<PayoutRequestResponseDto> response = payoutRequestService.getPayoutRequests(criteria, page, pageSize);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(response)
                .build());
    }

    @GetMapping("/own")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy danh sách yêu cầu rút tiền (Own)",
            description = "Lọc danh sách phân trang theo status, accountId, khoảng ngày tạo,...")
    public ResponseEntity<BaseResponse> getOwnPayoutRequests(
            @CurrentAccountId UUID accountId,
            @RequestParam Map<String, Object> criteria,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        criteria.put("accountId", accountId);
        BasePageResponse<PayoutRequestResponseDto> response = payoutRequestService.getPayoutRequests(criteria, page, pageSize);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(response)
                .build());
    }

    @PutMapping("/{id}/process")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Duyệt hoặc Từ chối yêu cầu rút tiền",
            description = "Truyền status là APPROVED hoặc REJECTED kèm ghi chú adminNote. Nếu từ chối, tiền sẽ được tự động hoàn lại ví Creator.")
    public ResponseEntity<BaseResponse> processPayoutRequest(
            @PathVariable("id") String payoutRequestId,
            @Valid @RequestBody PayoutRequestProcessDto dto) {

        PayoutRequestResponseDto response = payoutRequestService.processPayoutRequest(payoutRequestId, dto);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Xử lý yêu cầu rút tiền thành công")
                .data(response)
                .build());
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Thực thi chi trả tiền cho yêu cầu đã duyệt (Admin)",
            description = "Gọi PayoutService để gửi lệnh Batch Payout tới PayOS. Nếu thành công, chuyển PayoutRequest sang trạng thái PAID và tạo bản ghi WalletPayoutTransaction."
    )
    public ResponseEntity<BaseResponse> executePayout(@PathVariable("id") String payoutRequestId) {
        PayoutRequestResponseDto response = payoutRequestService.executePayout(payoutRequestId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Thực thi chi trả thành công")
                .data(response)
                .build());
    }
}