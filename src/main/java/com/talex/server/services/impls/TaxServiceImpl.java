package com.talex.server.services.impls;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.tax.AdminTaxSummaryResponseDto;
import com.talex.server.dtos.tax.CreatorTaxSummaryResponseDto;
import com.talex.server.dtos.tax.PitReportItemDto;
import com.talex.server.dtos.tax.VatReportItemDto;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.CreatorIdentity;
import com.talex.server.entities.creator.CreatorMonthlySettlement;
import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.enums.transaction.SettlementStatus;
import com.talex.server.repositories.creator.CreatorMonthlySettlementRepository;
import com.talex.server.repositories.creator.CreatorRepository;
import com.talex.server.repositories.transaction.OrderRepository;
import com.talex.server.services.TaxService;
import com.talex.server.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxServiceImpl implements TaxService {

    private final OrderRepository orderRepository;
    private final CreatorMonthlySettlementRepository settlementRepository;
    private final CreatorRepository creatorRepository;

    private static final String COMPANY_NAME = "CÔNG TY CỔ PHẦN NỀN TẢNG TALEX";
    private static final String COMPANY_TAX_CODE = "0101234567-demo";
    private static final String COMPANY_ADDRESS = "Tầng 10, Tòa nhà Talex Tower, Đường Demo, Q.1, TP.HCM";

    private static final List<String> PLATFORM_ITEM_TYPES = List.of("SUBSCRIPTION", "ENGAGEMENT");
    private static final List<String> CREATOR_ITEM_TYPES = List.of("EPISODE", "COMBO");
    private static final List<SettlementStatus> VALID_TAX_STATUSES = List.of(SettlementStatus.APPROVED, SettlementStatus.PAID);

    @Override
    @Transactional(readOnly = true)
    public AdminTaxSummaryResponseDto getAdminTaxSummary(int year, Integer quarter) {
        LocalDateTime startDate = getStartDate(year, quarter);
        LocalDateTime endDate = getEndDate(year, quarter);

        BigDecimal platformVat = Optional.ofNullable(
                orderRepository.sumVatByItemTypesAndDateRange(OrderStatus.COMPLETED, PLATFORM_ITEM_TYPES, startDate, endDate)
        ).orElse(BigDecimal.ZERO);

        BigDecimal creatorVat = Optional.ofNullable(
                orderRepository.sumVatByItemTypesAndDateRange(OrderStatus.COMPLETED, CREATOR_ITEM_TYPES, startDate, endDate)
        ).orElse(BigDecimal.ZERO);

        List<CreatorMonthlySettlement> settlements = settlementRepository.findForTaxByQuarter(
                startDate, endDate, VALID_TAX_STATUSES
        );

        BigDecimal totalGross = settlements.stream().map(CreatorMonthlySettlement::getGrossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPit = settlements.stream().map(s -> Optional.ofNullable(s.getTaxWithheldAmount()).orElse(BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNet = settlements.stream().map(CreatorMonthlySettlement::getNetPayoutAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return AdminTaxSummaryResponseDto.builder()
                .companyName(COMPANY_NAME)
                .enterpriseTaxCode(COMPANY_TAX_CODE)
                .companyAddress(COMPANY_ADDRESS)
                .platformVatAmount(platformVat)
                .creatorVatAmount(creatorVat)
                .totalVatAmount(platformVat.add(creatorVat))
                .totalGrossAmount(totalGross)
                .totalPitWithheld(totalPit)
                .totalNetPayout(totalNet)
                .totalSettlementsCount(settlements.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<VatReportItemDto> getVatReport(String itemType, LocalDateTime startDate, LocalDateTime endDate, int page, int pageSize) {
        LocalDateTime start = startDate != null ? startDate : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        Page<Order> orderPage = orderRepository.filterVatOrders(
                OrderStatus.COMPLETED, itemType, start, end, PageRequest.of(page - 1, pageSize)
        );

        List<VatReportItemDto> items = orderPage.getContent().stream().map(o -> VatReportItemDto.builder()
                .orderId(o.getOrderId())
                .itemType(o.getItemType())
                .itemId(o.getItemId())
                .fiatAmount(o.getTotalAmount().subtract(o.getVatAmount()))
                .totalAmount(o.getTotalAmount())
                .vatRate(o.getVatRate())
                .vatAmount(o.getVatAmount())
                .paymentCode(o.getPaymentCode())
                .createdAt(o.getCreatedAt())
                .revenueGroup(PLATFORM_ITEM_TYPES.contains(o.getItemType()) ? "PLATFORM" : "CREATOR")
                .build()
        ).toList();

        return BasePageResponse.<VatReportItemDto>builder()
                .content(items)
                .pageNumber(orderPage.getNumber() + 1)
                .pageSize(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .isFirst(orderPage.isFirst())
                .isLast(orderPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<PitReportItemDto> getPitReport(String yearMonth, String statusStr, int page, int pageSize) {
        SettlementStatus status = statusStr != null ? SettlementStatus.valueOf(statusStr) : null;
        Page<CreatorMonthlySettlement> settlementPage = settlementRepository.filterPitSettlements(
                yearMonth, status, PageRequest.of(page - 1, pageSize)
        );

        List<PitReportItemDto> items = settlementPage.getContent().stream().map(this::mapToPitItemDto).toList();

        return BasePageResponse.<PitReportItemDto>builder()
                .content(items)
                .pageNumber(settlementPage.getNumber() + 1)
                .pageSize(settlementPage.getSize())
                .totalElements(settlementPage.getTotalElements())
                .totalPages(settlementPage.getTotalPages())
                .isFirst(settlementPage.isFirst())
                .isLast(settlementPage.isLast())
                .build();
    }

    /**
     * MẪU 1 (ADMIN): Bảng kê 05-2/BK-TNCN theo Thông tư 80/2021/TT-BTC
     * Dùng cho cá nhân vãng lai / ký hợp đồng dịch vụ không cư trú / khấu trừ 10%
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportBk052PitExcel(int taxYear) {
        List<CreatorMonthlySettlement> settlements = settlementRepository.findForTaxByYear(
                String.valueOf(taxYear), VALID_TAX_STATUSES
        );

        StringBuilder csv = new StringBuilder();
        // Chèn UTF-8 BOM để Excel hiển thị tiếng Việt không lỗi font
        csv.append("\uFEFF");
        csv.append("STT,Họ và Tên,Mã Số Thuế,Số CCCD/CMND,Tổng Thu Nhập Chịu Thuế (Gross),Thuế Suất (%),Số Thuế TNCN Đã Khấu Trừ,Thu Nhập Thực Nhận (Net),Trạng Thái\n");

        int index = 1;
        for (CreatorMonthlySettlement s : settlements) {
            CreatorIdentity identity = Optional.ofNullable(s.getCreator()).map(Creator::getCreatorIdentity).orElse(null);
            String name = identity != null && !ValidationUtils.isNullOrEmpty(identity.getFullName()) ? identity.getFullName() : "N/A";
            String taxId = identity != null && !ValidationUtils.isNullOrEmpty(identity.getTaxId()) ? identity.getTaxId() : "N/A";
            String idNum = identity != null && !ValidationUtils.isNullOrEmpty(identity.getIdNumber()) ? identity.getIdNumber() : "N/A";

            csv.append(String.format("%d,\"%s\",\"%s\",\"%s\",%.2f,%.2f,%.2f,%.2f,\"%s\"\n",
                    index++, name, taxId, idNum,
                    s.getGrossAmount(),
                    Optional.ofNullable(s.getTaxRate()).orElse(10.0),
                    Optional.ofNullable(s.getTaxWithheldAmount()).orElse(BigDecimal.ZERO),
                    Optional.ofNullable(s.getNetPayoutAmount()).orElse(BigDecimal.ZERO),
                    s.getStatus().name()
                    ));
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * MẪU 2 (ADMIN): Bảng kê Hóa đơn, Chứng từ Hàng hóa Dịch vụ Bán ra (Tờ khai 01/GTGT)
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportVatExcel(LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime start = startDate != null ? startDate : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        Page<Order> orders = orderRepository.filterVatOrders(OrderStatus.COMPLETED, null, start, end, PageRequest.of(0, 10000));

        StringBuilder csv = new StringBuilder();
        csv.append("\uFEFF");
        csv.append("Mã Đơn Hàng,Loại Sản Phẩm,Tổng Doanh Thu,Thuế Suất VAT (%),Tiền Thuế VAT,Số Coin Quy Đổi,Doanh Thu Rong,Ngày Tạo\n");

        for (Order o : orders.getContent()) {
            csv.append(String.format("\"%s\",\"%s\",%.2f,%.2f,%.2f,%d,%.2f,\"%s\"\n",
                    o.getOrderId(), o.getItemType(), o.getTotalAmount(),
                    Optional.ofNullable(o.getVatRate()).orElse(0.0),
                    Optional.ofNullable(o.getVatAmount()).orElse(BigDecimal.ZERO),
                    o.getCoinAmount(),
                    o.getTotalAmount().subtract(Optional.ofNullable(o.getVatAmount()).orElse(BigDecimal.ZERO)),
                    o.getCreatedAt()));
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @Transactional(readOnly = true)
    public CreatorTaxSummaryResponseDto getCreatorTaxSummary(String creatorId, int year) {
        Creator creator = creatorRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Creator không tồn tại"));

        CreatorIdentity identity = creator.getCreatorIdentity();

        List<CreatorMonthlySettlement> settlements = settlementRepository.findForTaxByYearAndCreator(
                String.valueOf(year), List.of(SettlementStatus.PAID), creatorId
        );

        BigDecimal totalGross = settlements.stream().map(CreatorMonthlySettlement::getGrossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPit = settlements.stream().map(s -> Optional.ofNullable(s.getTaxWithheldAmount()).orElse(BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNet = settlements.stream().map(CreatorMonthlySettlement::getNetPayoutAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PitReportItemDto> details = settlements.stream().map(this::mapToPitItemDto).toList();

        return CreatorTaxSummaryResponseDto.builder()
                .creatorId(creatorId)
                .fullName(identity != null ? ValidationUtils.isNullOrEmpty(identity.getFullName()) ? "N/A" : identity.getFullName() : "N/A")
                .taxId(identity != null ? ValidationUtils.isNullOrEmpty(identity.getTaxId()) ? "N/A" : identity.getTaxId() : "N/A")
                .idNumber(identity != null ? ValidationUtils.isNullOrEmpty(identity.getIdNumber()) ? "N/A" : identity.getIdNumber() : "N/A")
                .taxYear(year)
                .totalGrossAmount(totalGross)
                .totalPitWithheld(totalPit)
                .totalNetPayout(totalNet)
                .monthlyDetails(details)
                .build();
    }

    /**
     * MẪU 3 (CREATOR): Chứng từ Khấu trừ Thuế TNCN Điện tử dạng PDF
     * Theo Nghị định 123/2020/NĐ-CP và Thông tư 78/2021/TT-BTC
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportCreatorTaxCertificatePdf(String creatorId, int year) {
        CreatorTaxSummaryResponseDto summary = getCreatorTaxSummary(creatorId, year);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Đọc file Font Tiếng Việt từ classpath (resources/fonts/)
            byte[] regularFontBytes;
            byte[] boldFontBytes;

            try (InputStream regStream = new ClassPathResource("fonts/Arial.TTF").getInputStream();
                 InputStream boldStream = new ClassPathResource("fonts/Arial-Bold.TTF").getInputStream()) {
                regularFontBytes = regStream.readAllBytes();
                boldFontBytes = boldStream.readAllBytes();
            }

            // 2. Khởi tạo BaseFont hỗ trợ UTF-8 (IDENTITY_H) và EMBEDDED (nhúng font vào PDF)
            BaseFont baseRegular = BaseFont.createFont("Arial.TTF", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, regularFontBytes, null);
            BaseFont baseBold = BaseFont.createFont("Arial-Bold.TTF", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, boldFontBytes, null);

            Font titleFont = new Font(baseBold, 13);
            Font headerFont = new Font(baseBold, 10);
            Font normalFont = new Font(baseRegular, 10);
            Font italicFont = new Font(baseRegular, 9, Font.ITALIC);

            // 3. Quốc hiệu & Tiêu đề
            Paragraph title = new Paragraph("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM\nĐộc lập - Tự do - Hạnh phúc\n\n", headerFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph docName = new Paragraph("CHỨNG TỪ KHẤU TRỪ THUẾ THU NHẬP CÁ NHÂN\n(Theo NĐ 123/2020/NĐ-CP & TT 78/2021/TT-BTC)\n\n", titleFont);
            docName.setAlignment(Element.ALIGN_CENTER);
            document.add(docName);

            // 4. Bên chi trả thu nhập
            Paragraph compInfo = new Paragraph();
            compInfo.setFont(normalFont);
            compInfo.add(new Chunk("I. TỔ CHỨC CHI TRẢ THU NHẬP\n", headerFont));
            compInfo.add("1. Tên tổ chức: " + COMPANY_NAME + "\n");
            compInfo.add("2. Mã số thuế: " + COMPANY_TAX_CODE + "\n");
            compInfo.add("3. Địa chỉ: " + COMPANY_ADDRESS + "\n\n");
            document.add(compInfo);

            // 5. Bên nhận thu nhập
            Paragraph creatorInfo = new Paragraph();
            creatorInfo.setFont(normalFont);
            creatorInfo.add(new Chunk("II. CÁ NHÂN NHẬN THU NHẬP (CREATOR)\n", headerFont));
            creatorInfo.add("1. Họ và tên: " + summary.getFullName() + "\n");
            creatorInfo.add("2. Mã số thuế cá nhân: " + summary.getTaxId() + "\n");
            creatorInfo.add("3. Số CCCD/CMND: " + summary.getIdNumber() + "\n");
            creatorInfo.add("4. Năm tính thuế: " + summary.getTaxYear() + "\n\n");
            document.add(creatorInfo);

            // 6. Bảng chi tiết
            Paragraph sec3 = new Paragraph("III. THÔNG TIN THUẾ KHẤU TRỪ\n", headerFont);
            sec3.setSpacingAfter(5);
            document.add(sec3);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);

            addTableCell(table, "Chỉ tiêu", headerFont, true);
            addTableCell(table, "Số tiền (VND)", headerFont, true);

            addTableCell(table, "Tổng thu nhập chịu thuế đã trả (Gross)", normalFont, false);
            addTableCell(table, String.format("%,.2f", summary.getTotalGrossAmount()), normalFont, false);

            addTableCell(table, "Tổng số thuế TNCN đã khấu trừ", normalFont, false);
            addTableCell(table, String.format("%,.2f", summary.getTotalPitWithheld()), normalFont, false);

            addTableCell(table, "Số tiền còn lại thực nhận (Net)", headerFont, false);
            addTableCell(table, String.format("%,.2f", summary.getTotalNetPayout()), headerFont, false);

            document.add(table);

            // 7. Chữ ký & Ngày xuất
            String currentDateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("'Ngày 'dd' tháng 'MM' năm 'yyyy"));
            Paragraph footerDate = new Paragraph("\n\n" + currentDateStr + "\n", italicFont);
            footerDate.setAlignment(Element.ALIGN_RIGHT);
            document.add(footerDate);

            Paragraph signBlock = new Paragraph("ĐẠI DIỆN TỔ CHỨC CHI TRẢ THU NHẬP\n(Ký, ghi rõ họ tên & đóng dấu điện tử)\n", headerFont);
            signBlock.setAlignment(Element.ALIGN_RIGHT);
            document.add(signBlock);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Lỗi khi sinh PDF chứng từ thuế cho creatorId {}: {}", creatorId, e.getMessage());
            throw new RuntimeException("Không thể xuất chứng từ PDF: " + e.getMessage());
        }
    }

    private void addTableCell(PdfPTable table, String text, Font font, boolean isHeader) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        if (isHeader) {
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        }
        table.addCell(cell);
    }

    private PitReportItemDto mapToPitItemDto(CreatorMonthlySettlement s) {
        CreatorIdentity identity = Optional.ofNullable(s.getCreator()).map(Creator::getCreatorIdentity).orElse(null);

        return PitReportItemDto.builder()
                .settlementId(s.getCreatorMonthlySettlementId())
                .settlementMonth(s.getSettlementMonth())
                .creatorId(s.getCreator() != null ? s.getCreator().getCreatorId() : null)
                .creatorFullName(identity != null ? identity.getFullName() : "N/A")
                .taxId(identity != null ? identity.getTaxId() : "N/A")
                .idNumber(identity != null ? identity.getIdNumber() : "N/A")
                .grossAmount(s.getGrossAmount())
                .taxRate(s.getTaxRate())
                .taxWithheldAmount(s.getTaxWithheldAmount())
                .netPayoutAmount(s.getNetPayoutAmount())
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .createdAt(s.getCreatedAt())
                .build();
    }

    private LocalDateTime getStartDate(int year, Integer quarter) {
        if (quarter == null) return LocalDateTime.of(year, 1, 1, 0, 0);
        int startMonth = (quarter - 1) * 3 + 1;
        return LocalDateTime.of(year, startMonth, 1, 0, 0);
    }

    private LocalDateTime getEndDate(int year, Integer quarter) {
        if (quarter == null) return LocalDateTime.of(year, 12, 31, 23, 59, 59);
        int endMonth = quarter * 3;
        int lastDay = (endMonth == 4 || endMonth == 6 || endMonth == 9 || endMonth == 11) ? 30 : (endMonth == 2 ? 28 : 31);
        return LocalDateTime.of(year, endMonth, lastDay, 23, 59, 59);
    }
}