package com.talex.server.services;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.tax.*;

import java.time.LocalDateTime;

public interface TaxService {
    AdminTaxSummaryResponseDto getAdminTaxSummary(int year, Integer quarter);

    BasePageResponse<VatReportItemDto> getVatReport(String itemType, LocalDateTime startDate, LocalDateTime endDate, int page, int pageSize);

    BasePageResponse<PitReportItemDto> getPitReport(String yearMonth, String status, int page, int pageSize);

    // Đã đổi tên hàm và mẫu đúng: Phụ lục 05-2/BK-TNCN theo TT 80/2021/TT-BTC
    byte[] exportBk052PitExcel(int taxYear);

    // Bảng kê thuế GTGT bán ra
    byte[] exportVatExcel(LocalDateTime startDate, LocalDateTime endDate);

    CreatorTaxSummaryResponseDto getCreatorTaxSummary(String creatorId, int year);

    // Xuất Chứng từ Khấu trừ Thuế TNCN dạng PDF theo NĐ 123/2020/NĐ-CP
    byte[] exportCreatorTaxCertificatePdf(String creatorId, int year);
}