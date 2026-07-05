package com.talex.server.controllers.creator;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.creator.CreatorIdentityRequestDto;
import com.talex.server.dtos.requests.creator.CreatorVerifiedResultDto;
import com.talex.server.dtos.requests.filters.CreatorIdentityFilterRequestDto;
import com.talex.server.dtos.responses.CreatorIdentityResponseDto;
import com.talex.server.services.creator.ICreatorIdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/creators/identities")
@RequiredArgsConstructor
@Tag(name = "Creator Identities", description = "API quản lý thông tin định danh của creator")
public class CreatorIdentityController {
    private final ICreatorIdentityService creatorIdentityService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    @Operation(summary = "Lấy danh tính creator theo ID", description = "Trả về thông tin định danh creator theo ID.")
    public ResponseEntity<BaseResponse> getById(@PathVariable String id) {
        CreatorIdentityResponseDto resp = creatorIdentityService.getById(id);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(resp)
                .build());
    }

    @GetMapping("/own")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy danh tính creator của tài khoản", description = "Lấy thông tin định danh creator liên kết với tài khoản đang đăng nhập.")
    public ResponseEntity<BaseResponse> getAccountCreatorIdentity(
            @CurrentAccountId UUID accountId) {
        CreatorIdentityResponseDto resp = creatorIdentityService
                .getByAccountId(accountId.toString());
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(resp)
                .build());
    }

    @PutMapping("/tax")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cập nhật mã số thuế cho creator", description = "Cập nhật thông tin mã số thuế cho creator.")
    public ResponseEntity<BaseResponse> registerTaxId(
            @CurrentAccountId UUID accountId,
            @RequestBody CreatorIdentityRequestDto dto
    ) {
        String resp = creatorIdentityService
                .updateTaxId(accountId, dto.getTaxId());
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message(resp)
                .data(resp)
                .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Cập nhật danh tính creator", description = "Cập nhật thông tin định danh creator theo ID.")
    public ResponseEntity<BaseResponse> update(
            @PathVariable String id,
            @RequestBody CreatorIdentityRequestDto dto) {
        CreatorIdentityResponseDto resp = creatorIdentityService.update(id, dto);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Updated")
                .data(resp)
                .build());
    }

    @PutMapping("/verification/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Cập nhật kết quả kiểm tra danh tính creator", description = "Cập nhật kết quả kiếm tra danh tính creator theo ID.")
    public ResponseEntity<BaseResponse> updateVerifyResult(
            @PathVariable String id,
            @RequestBody CreatorVerifiedResultDto dto) {
        creatorIdentityService.updateVerifiedStatus(id, dto);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Updated")
                .data(null)
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa danh tính creator", description = "Xóa thông tin định danh creator theo ID, chỉ admin được phép.")
    public ResponseEntity<BaseResponse> delete(@PathVariable String id) {
        creatorIdentityService.delete(id);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Deleted")
                .data(null)
                .build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(
            summary = "Lọc nâng cao danh sách danh tính creator",
            description = "Tìm kiếm kết hợp phân trang, sắp xếp và lọc động theo nhiều khoảng dữ liệu dựa trên tiêu chí cấu hình."
    )
    public ResponseEntity<BaseResponse> filter(
            @RequestParam(required = false) String[] statuses,
            @RequestParam(required = false) Map<String, Object> criteria,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        BasePageResponse<CreatorIdentityResponseDto> pageResponse = creatorIdentityService
                .filter(CreatorIdentityFilterRequestDto.builder()
                        .criteria(criteria)
                        .statuses(statuses)
                        .sortBy(sortBy)
                        .sortDirection(sortDirection)
                        .page(page)
                        .pageSize(pageSize)
                        .build());

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(pageResponse)
                .build());
    }
}
