package com.talex.server.controllers.creator;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.tax.CreatorTaxSummaryResponseDto;
import com.talex.server.services.TaxService;
import com.talex.server.services.creator.CreatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/creator/tax")
@RequiredArgsConstructor
@Tag(name = "Creator Tax Management", description = "Các API Tra cứu Thuế dành cho Creator")
public class CreatorTaxController {

    private final TaxService taxService;
    private final CreatorService creatorService;

    @GetMapping("/my-summary")
    @Operation(summary = "1. API Xem tổng quan nghĩa vụ thuế cá nhân theo năm của Creator")
    public ResponseEntity<BaseResponse> getMyTaxSummary(
            @CurrentAccountId UUID accountId,
            @RequestParam(defaultValue = "2026") int year
    ) {
        String creatorId = creatorService.getIdByAccountId(accountId);
        CreatorTaxSummaryResponseDto data = taxService.getCreatorTaxSummary(creatorId, year);
        return ResponseEntity.ok(BaseResponse.builder().code(200).message("Thành công").data(data).build());
    }

    @GetMapping("/export-certificate")
    @Operation(summary = "2. API Tải Chứng từ Khấu trừ Thuế TNCN Điện tử (File PDF)")
    public ResponseEntity<byte[]> exportTaxCertificate(
            @CurrentAccountId UUID accountId,
            @RequestParam(defaultValue = "2026") int year
    ) {
        String creatorId = creatorService.getIdByAccountId(accountId);
        byte[] pdfData = taxService.exportCreatorTaxCertificatePdf(creatorId, year);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Chung_Tu_Thue_TNCN_" + year + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }
}