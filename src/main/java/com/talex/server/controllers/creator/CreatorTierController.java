package com.talex.server.controllers.creator;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.creator.CreatorTierRequestDto;
import com.talex.server.dtos.requests.filters.CreatorTierFilterRequestDto;
import com.talex.server.dtos.responses.CreatorTierResponseDto;
import com.talex.server.services.creator.ICreatorTierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/creator-tiers")
@RequiredArgsConstructor
@Tag(name = "Creator Tiers", description = "API quản lý cấp độ của người sáng tạo nội dung")
public class CreatorTierController {
    private final ICreatorTierService creatorTierService;

    @PostMapping
//    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo cấp độ creator mới", description = "Tạo cấp độ creator mới")
    public ResponseEntity<BaseResponse> create(@Valid @RequestBody CreatorTierRequestDto dto) {
        CreatorTierResponseDto resp = creatorTierService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .code(201)
                        .message("Created")
                        .data(resp)
                        .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy cấp độ creator theo ID", description = "Trả về thông tin cấp độ creator theo ID.")
    public ResponseEntity<BaseResponse> getById(@PathVariable String id) {
        CreatorTierResponseDto resp = creatorTierService.getById(id);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(resp)
                .build());
    }

    @GetMapping
    @Operation(summary = "Tìm kiếm/Lọc cấp độ creator", description = "Tìm kiếm và phân trang các cấp độ creator theo tiêu chí lọc.")
    public ResponseEntity<BaseResponse> search(
            @RequestParam(required = false) Map<String, Object> criteria,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        BasePageResponse<CreatorTierResponseDto> pageResp = creatorTierService.list(
                CreatorTierFilterRequestDto.builder()
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

    @PutMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật cấp độ creator", description = "Cập nhật thông tin cấp độ creator theo ID")
    public ResponseEntity<BaseResponse> update(
            @PathVariable String id,
            @Valid @RequestBody CreatorTierRequestDto dto
    ) {
        CreatorTierResponseDto resp = creatorTierService.update(id, dto);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Updated")
                .data(resp)
                .build());
    }

    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa cấp độ creator", description = "Xóa cấp độ creator theo ID (soft delete)")
    public ResponseEntity<BaseResponse> delete(@PathVariable String id) {
        creatorTierService.delete(id);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Deleted")
                .data(null)
                .build());
    }
}
