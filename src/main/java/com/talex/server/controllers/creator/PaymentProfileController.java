package com.talex.server.controllers.creator;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.creator.PaymentProfileRequestDto;
import com.talex.server.dtos.requests.creator.PaymentProfileVerifiedDto;
import com.talex.server.dtos.requests.filters.PaymentProfileFilterRequestDto;
import com.talex.server.dtos.responses.PaymentProfileResponseDto;
import com.talex.server.services.creator.IPaymentProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment-profiles")
@RequiredArgsConstructor
@Tag(name = "Payment Profiles", description = "API quản lý hồ sơ thanh toán của creator")
public class PaymentProfileController {
    private final IPaymentProfileService paymentProfileService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    @Operation(summary = "Tạo hồ sơ thanh toán mới", description = "Tạo hồ sơ thanh toán mới cho creator")
    public ResponseEntity<BaseResponse> create(
            @CurrentAccountId UUID accountId,
            @Valid @RequestBody PaymentProfileRequestDto dto
    ) {
        PaymentProfileResponseDto resp = paymentProfileService.create(accountId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .code(201)
                        .message("Created")
                        .data(resp)
                        .build());
    }

//    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @GetMapping("/{id}")
    @Operation(summary = "Lấy hồ sơ thanh toán theo ID", description = "Trả về thông tin hồ sơ thanh toán theo ID")
    public ResponseEntity<BaseResponse> getById(@PathVariable String id) {
        PaymentProfileResponseDto resp = paymentProfileService.getById(id);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(resp)
                .build());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/own/primary")
    @Operation(summary = "Lấy hồ sơ thanh toán chính của creator", description = "Trả về hồ sơ thanh toán chính cho creator")
    public ResponseEntity<BaseResponse> getPrimaryByAccountId(
            @CurrentAccountId UUID accountId
    ) {
        PaymentProfileResponseDto resp = paymentProfileService.getPrimaryProfile(accountId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(resp)
                .build());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/own")
    @Operation(summary = "Lấy tất cả hồ sơ thanh toán của creator", description = "Trả về danh sách tất cả hồ sơ thanh toán của creator")
    public ResponseEntity<BaseResponse> getOwnerProfiles(
            @CurrentAccountId UUID accountId
    ) {
        List<PaymentProfileResponseDto> resp = paymentProfileService.getOwnProfiles(accountId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(resp)
                .build());
    }

    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @GetMapping
    @Operation(summary = "Tìm kiếm/Lọc hồ sơ thanh toán", description = "Tìm kiếm và phân trang các hồ sơ thanh toán theo tiêu chí lọc")
    public ResponseEntity<BaseResponse> search(
            @RequestParam(required = false) String creatorId,
            @RequestParam(required = false) Map<String, Object> criteria,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        BasePageResponse<PaymentProfileResponseDto> pageResp = paymentProfileService.list(
                PaymentProfileFilterRequestDto.builder()
                        .creatorId(creatorId)
                        .criteria(criteria)
                        .sortBy(sortBy)
                        .sortDirection(sortDirection)
                        .page(page)
                        .pageSize(pageSize)
                        .build()
        );

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(pageResp)
                .build());
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật hồ sơ thanh toán", description = "Cập nhật thông tin hồ sơ thanh toán theo ID")
    public ResponseEntity<BaseResponse> update(
            @PathVariable String id,
            @Valid @RequestBody PaymentProfileRequestDto dto
    ) {
        PaymentProfileResponseDto resp = paymentProfileService.update(id, dto);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Updated")
                .data(resp)
                .build());
    }

//    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @PutMapping("/verification/{id}")
    @Operation(summary = "Cập nhật kiểm duyệt hồ sơ thanh toán", description = "Cập nhật kiểm duyệt thông tin hồ sơ thanh toán theo ID")
    public ResponseEntity<BaseResponse> updateVerifiedStatus(
            @PathVariable String id,
            @Valid @RequestBody PaymentProfileVerifiedDto dto
    ) {
        PaymentProfileResponseDto resp = paymentProfileService.updateVerifiedStatus(id, dto);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Updated")
                .data(resp)
                .build());
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa hồ sơ thanh toán", description = "Xóa hồ sơ thanh toán theo ID")
    public ResponseEntity<BaseResponse> delete(@PathVariable String id) {
        paymentProfileService.delete(id);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Deleted")
                .data(null)
                .build());
    }
}
