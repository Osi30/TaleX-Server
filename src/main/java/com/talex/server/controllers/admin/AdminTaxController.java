package com.talex.server.controllers.admin;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.tax.*;
import com.talex.server.services.TaxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/tax")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Tax Management", description = "Các API Quản lý và Xuất Báo cáo Thuế dành cho Admin")
public class AdminTaxController {

    private final TaxService taxService;

    @GetMapping("/summary")
    @Operation(summary = "1. API Tổng quan Thuế VAT & PIT dành cho Admin Dashboard")
    public ResponseEntity<BaseResponse> getSummary(
            @RequestParam(defaultValue = "2026") int year,
            @RequestParam(required = false) Integer quarter
    ) {
        AdminTaxSummaryResponseDto data = taxService.getAdminTaxSummary(year, quarter);
        return ResponseEntity.ok(BaseResponse.builder().code(200).message("Thành công").data(data).build());
    }

    @GetMapping("/vat-report")
    @Operation(summary = "2. API Báo cáo Chi tiết Thuế VAT theo đơn hàng")
    public ResponseEntity<BaseResponse> getVatReport(
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        BasePageResponse<VatReportItemDto> data = taxService.getVatReport(itemType, startDate, endDate, page, pageSize);
        return ResponseEntity.ok(BaseResponse.builder().code(200).message("Thành công").data(data).build());
    }

    @GetMapping("/pit-report")
    @Operation(summary = "3. API Báo cáo Chi tiết Thuế TNCN (PIT) của Creator")
    public ResponseEntity<BaseResponse> getPitReport(
            @RequestParam(required = false) String yearMonth,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        BasePageResponse<PitReportItemDto> data = taxService.getPitReport(yearMonth, status, page, pageSize);
        return ResponseEntity.ok(BaseResponse.builder().code(200).message("Thành công").data(data).build());
    }

    @GetMapping("/export/bk05-2")
    @Operation(summary = "4. API Export Excel Bảng kê 05-2/BK-TNCN (Thông tư 80/2021/TT-BTC)")
    public ResponseEntity<byte[]> exportBk052(@RequestParam(defaultValue = "2026") int taxYear) {
        byte[] fileData = taxService.exportBk052PitExcel(taxYear);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Bang_Ke_05-2_BK-TNCN_" + taxYear + ".csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(fileData);
    }

    @GetMapping("/export/vat-excel")
    @Operation(summary = "5. API Export Excel Bảng kê Thuế VAT Bán ra")
    public ResponseEntity<byte[]> exportVatExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        byte[] fileData = taxService.exportVatExcel(startDate, endDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Bao_Cao_VAT_Ban_Ra.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(fileData);
    }
}