package com.talex.server.controllers.campaign;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.campaign.EngagementServiceRequestDto;
import com.talex.server.dtos.requests.filters.EngagementServiceFilterRequestDto;
import com.talex.server.dtos.responses.campaign.EngagementServiceResponseDto;
import com.talex.server.services.campaign.IEngagementServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/engagement-services")
@RequiredArgsConstructor
@Tag(name = "Engagement Services", description = "API quản lý các loại dịch vụ tương tác dùng trong chiến dịch")
public class EngagementServiceController {
    private final IEngagementServiceService engagementServiceService;

    @PostMapping
    @Operation(
            summary = "Tạo dịch vụ tương tác mới",
            description = "Tạo một dịch vụ tương tác để sử dụng trong chiến dịch quảng cáo."
    )
    public ResponseEntity<BaseResponse> create(
           @Valid @RequestBody EngagementServiceRequestDto request
    ) {
        EngagementServiceResponseDto response = engagementServiceService
                .createEngagementService(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .code(201)
                        .message("Engagement service created")
                        .data(response)
                        .build());
    }

    @GetMapping("/search")
    @Operation(summary = "Lấy danh sách dịch vụ tương tác", description = "Lọc và phân trang các dịch vụ tương tác theo trạng thái, id và từ khóa.")
    public ResponseEntity<BaseResponse> list(
            @RequestParam(required = false) String[] types,
            @RequestParam(required = false) String[] targets,
            @RequestParam(required = false) Map<String, Object> criteria,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        BasePageResponse<EngagementServiceResponseDto> pageResponse = engagementServiceService
                .filterEngagementServices(EngagementServiceFilterRequestDto.builder()
                        .criteria(criteria)
                        .types(types)
                        .targets(targets)
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

    @GetMapping("/{engagementServiceId}")
    @Operation(summary = "Lấy dịch vụ tương tác theo ID", description = "Trả về chi tiết dịch vụ tương tác theo id.")
    public ResponseEntity<BaseResponse> getById(@PathVariable String engagementServiceId) {
        EngagementServiceResponseDto response = engagementServiceService
                .getEngagementServiceById(engagementServiceId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(response)
                .build());
    }

    @PutMapping("/{engagementServiceId}")
    @Operation(summary = "Cập nhật dịch vụ tương tác", description = "Cập nhật thông tin dịch vụ tương tác theo id.")
    public ResponseEntity<BaseResponse> update(
            @PathVariable String engagementServiceId,
            @RequestBody EngagementServiceRequestDto request
    ) {
        EngagementServiceResponseDto response = engagementServiceService
                .updateEngagementService(engagementServiceId, request);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Engagement service updated")
                .data(response)
                .build());
    }

    @DeleteMapping("/{engagementServiceId}")
    @Operation(summary = "Xóa dịch vụ tương tác", description = "Xóa dịch vụ tương tác theo id.")
    public ResponseEntity<BaseResponse> delete(@PathVariable String engagementServiceId) {
        engagementServiceService.deleteEngagementService(engagementServiceId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Engagement service deleted")
                .data(null)
                .build());
    }
}
