package com.talex.server.controllers.creator;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.tax.CreatorTaxSummaryResponseDto;
import com.talex.server.services.TaxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/creator/tax")
@RequiredArgsConstructor
@Tag(name = "Creator Tax Management", description = "Các API Tra cứu Thuế dành cho Creator")
public class CreatorTaxController {

    private final TaxService taxService;

    @GetMapping("/my-summary/{creatorId}")
    @Operation(summary = "1. API Xem tổng quan nghĩa vụ thuế cá nhân theo năm của Creator")
    public ResponseEntity<BaseResponse> getMyTaxSummary(
            @PathVariable String creatorId,
            @RequestParam(defaultValue = "2026") int year
    ) {
        CreatorTaxSummaryResponseDto data = taxService.getCreatorTaxSummary(creatorId, year);
        return ResponseEntity.ok(BaseResponse.builder().code(200).message("Thành công").data(data).build());
    }

    @GetMapping("/export-certificate/{creatorId}")
    @Operation(summary = "2. API Tải Chứng từ Khấu trừ Thuế TNCN Điện tử (File PDF)")
    public ResponseEntity<byte[]> exportTaxCertificate(
            @PathVariable String creatorId,
            @RequestParam(defaultValue = "2026") int year
    ) {
        byte[] pdfData = taxService.exportCreatorTaxCertificatePdf(creatorId, year);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Chung_Tu_Thue_TNCN_" + year + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }
}