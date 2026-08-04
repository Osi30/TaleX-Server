package com.talex.server.controllers.report;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.annotations.CurrentRole;
import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.report.request.ReportRequestDto;
import com.talex.server.dtos.report.response.ReportResponseDto;
import com.talex.server.services.report.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "API quản lý báo cáo vi phạm dành cho người dùng")
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Gửi báo cáo vi phạm",
            description = "Người dùng gửi báo cáo vi phạm cho bài viết, bình luận, series hoặc tài khoản."
    )
    public ResponseEntity<BaseResponse> createReport(
            @CurrentAccountId UUID accountId,
            @CurrentRole String role,
            @Valid @RequestBody ReportRequestDto request
    ) {
        ReportResponseDto response = reportService.createReport(
                accountId.toString(),
                role,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .code(201)
                        .message("Báo cáo vi phạm đã được gửi thành công")
                        .data(response)
                        .build());
    }

    @GetMapping("/my-reports")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Danh sách báo cáo của tôi",
            description = "Lấy danh sách các báo cáo vi phạm mà người dùng hiện tại đã gửi."
    )
    public ResponseEntity<BaseResponse> getMyReports(
            @CurrentAccountId UUID accountId,
            @RequestParam(required = false) Map<String, Object> criteria,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        BasePageResponse<ReportResponseDto> pageResponse = reportService.getUserReports(
                accountId.toString(),
                BaseFilterRequestDto.builder()
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
                .data(pageResponse)
                .build());
    }
}