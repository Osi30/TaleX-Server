package com.talex.server.services.statistic.impls;

import com.talex.server.dtos.statistics.StatisticOverviewDto;
import com.talex.server.dtos.statistics.StatisticResponseDto;
import com.talex.server.dtos.statistics.StatisticTrendDto;
import com.talex.server.dtos.statistics.campaign.CampaignRevenueDetailDto;
import com.talex.server.dtos.statistics.campaign.CampaignRevenueOverviewDto;
import com.talex.server.dtos.statistics.campaign.CampaignStatisticData;
import com.talex.server.dtos.statistics.content.ContentRevenueDetailDto;
import com.talex.server.dtos.statistics.content.ContentRevenueOverviewDto;
import com.talex.server.dtos.statistics.content.ContentRevenueStatisticData;
import com.talex.server.dtos.statistics.subscription.SubscriptionRevenueDetailDto;
import com.talex.server.dtos.statistics.subscription.SubscriptionRevenueOverviewDto;
import com.talex.server.dtos.statistics.subscription.SubscriptionStatisticData;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.records.OrderDetailStatisticProjection;
import com.talex.server.records.OrderStatisticData;
import com.talex.server.repositories.transaction.OrderRepository;
import com.talex.server.services.statistic.StatisticService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticServiceImpl implements StatisticService {

    private final OrderRepository orderRepository;
    private static final String CAMPAIGN_ITEM_TYPE = "ENGAGEMENT";
    private static final String SUBSCRIPTION_ITEM_TYPE = "SUBSCRIPTION";

    @Override
    public StatisticResponseDto getOrderStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Xử lý giá trị mặc định nếu bỏ trống (6 tháng gần nhất)
        if (endTime == null) {
            endTime = now;
        }
        if (startTime == null) {
            startTime = endTime.minusMonths(6);
        }

        // Validate thời gian
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Thời gian bắt đầu (startTime) phải nhỏ hơn thời gian kết thúc (endTime).");
        }

        // 2. Giới hạn không được vượt quá 1 năm (365/366 ngày)
        if (startTime.plusYears(1).isBefore(endTime)) {
            throw new IllegalArgumentException("Khung thời gian truy vấn tối đa chỉ được 1 năm.");
        }

        // 3. Tự động xác định kiểu gom nhóm theo ngày hay tháng (nhỏ hơn 2 tháng -> chia theo ngày)
        boolean isLessThanTwoMonths = startTime.plusMonths(2).isAfter(endTime);
        String dateFormatPattern = isLessThanTwoMonths ? "YYYY-MM-DD" : "YYYY-MM";

        // Chỉ thống kê các đơn hàng đã hoàn tất (COMPLETED)
        String completedStatus = OrderStatus.COMPLETED.name();

        // 4. Truy vấn dữ liệu Tổng quan (Overview)
        OrderStatisticData overviewProjection = orderRepository.getOverviewStatistic(
                completedStatus, startTime, endTime
        );

        StatisticOverviewDto overview = StatisticOverviewDto.builder()
                .gmv(overviewProjection != null ? overviewProjection.gmv() : java.math.BigDecimal.ZERO)
                .totalNetRevenue(overviewProjection != null ? overviewProjection.netRevenue() : java.math.BigDecimal.ZERO)
                .totalVat(overviewProjection != null ? overviewProjection.vatAmount() : java.math.BigDecimal.ZERO)
                .totalCoin(overviewProjection != null ? overviewProjection.totalCoin().longValue() : 0L)
                .build();

        // 5. Truy vấn dữ liệu theo thời gian (Trends)
        List<OrderStatisticData> trendProjections = orderRepository.getGroupedStatistics(
                completedStatus, startTime, endTime, dateFormatPattern
        );

        List<StatisticTrendDto> trends = trendProjections.stream()
                .map(proj -> StatisticTrendDto.builder()
                        .period(proj.period())
                        .gmv(proj.gmv())
                        .netRevenue(proj.netRevenue())
                        .vatAmount(proj.vatAmount())
                        .totalCoin(proj.totalCoin() != null ? proj.totalCoin().longValue() : 0L)
                        .build())
                .toList();

        return StatisticResponseDto.builder()
                .overview(overview)
                .trends(trends)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportAllExcel(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (endTime == null) endTime = now;
        if (startTime == null) startTime = endTime.minusMonths(6);

        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("startTime phải nhỏ hơn hoặc bằng endTime!");
        }

        String completedStatus = OrderStatus.COMPLETED.name();

        // 1. Dữ liệu tổng quan chung
        OrderStatisticData overviewData = orderRepository.getOverviewStatistic(completedStatus, startTime, endTime);

        // 2. Dữ liệu chi tiết 3 loại
        List<OrderDetailStatisticProjection> campaignDetails = orderRepository.getCampaignOrderDetails(
                completedStatus, CAMPAIGN_ITEM_TYPE, startTime, endTime
        );
        List<OrderDetailStatisticProjection> contentDetails = orderRepository.getContentOrderDetails(
                completedStatus, startTime, endTime
        );
        List<OrderDetailStatisticProjection> subscriptionDetails = orderRepository.getSubscriptionOrderDetails(
                completedStatus, SUBSCRIPTION_ITEM_TYPE, startTime, endTime
        );

        return buildAllExcelWorkbook(overviewData, campaignDetails, contentDetails, subscriptionDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignRevenueOverviewDto getCampaignOverview(LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);

        String completedStatus = OrderStatus.COMPLETED.name();
        CampaignStatisticData data = orderRepository.getCampaignOverviewStatistic(
                completedStatus, CAMPAIGN_ITEM_TYPE, startTime, endTime
        );

        if (data == null) {
            return CampaignRevenueOverviewDto.builder()
                    .totalGrossRevenue(BigDecimal.ZERO)
                    .totalVatAmount(BigDecimal.ZERO)
                    .totalNetRevenue(BigDecimal.ZERO)
                    .build();
        }

        return CampaignRevenueOverviewDto.builder()
                .totalGrossRevenue(data.grossRevenue())
                .totalVatAmount(data.vatAmount())
                .totalNetRevenue(data.netRevenue())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignRevenueDetailDto> getCampaignDetails(LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);

        // Tính khoảng cách giữa startTime và endTime để chọn định dạng gom nhóm
        long daysBetween = Duration.between(startTime, endTime).toDays();

        String dateFormatPattern;
        String groupUnit;

        if (daysBetween < 7) {
            dateFormatPattern = "YYYY-MM-DD HH24:00";
            groupUnit = "HOUR";
        } else if (daysBetween < 30) {
            dateFormatPattern = "YYYY-MM-DD";
            groupUnit = "DAY";
        } else if (startTime.plusMonths(12).isAfter(endTime)) { // Dưới 12 tháng
            dateFormatPattern = "YYYY-MM";
            groupUnit = "MONTH";
        } else { // Trên 12 tháng
            dateFormatPattern = "YYYY";
            groupUnit = "YEAR";
        }

        String completedStatus = OrderStatus.COMPLETED.name();
        List<CampaignStatisticData> rawDataList = orderRepository.getCampaignGroupedStatistics(
                completedStatus, CAMPAIGN_ITEM_TYPE, startTime, endTime, dateFormatPattern
        );

        return rawDataList.stream()
                .map(data -> CampaignRevenueDetailDto.builder()
                        .period(data.period())
                        .grossRevenue(data.grossRevenue())
                        .vatAmount(data.vatAmount())
                        .netRevenue(data.netRevenue())
                        .groupUnit(groupUnit)
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCampaignExcel(LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);
        CampaignRevenueOverviewDto overview = getCampaignOverview(startTime, endTime);
        List<OrderDetailStatisticProjection> details = orderRepository.getCampaignOrderDetails(
                OrderStatus.COMPLETED.name(), CAMPAIGN_ITEM_TYPE, startTime, endTime
        );
        return buildExcelWorkbook("BÁO CÁO DOANH THU CAMPAIGN", overviewDataToRows(overview), details);
    }

    @Override
    @Transactional(readOnly = true)
    public ContentRevenueOverviewDto getContentOverview(LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);

        String completedStatus = OrderStatus.COMPLETED.name();
        ContentRevenueStatisticData data = orderRepository.getContentOverviewStatistic(
                completedStatus, startTime, endTime
        );

        if (data == null) {
            return ContentRevenueOverviewDto.builder()
                    .totalGrossRevenue(BigDecimal.ZERO)
                    .totalVatAmount(BigDecimal.ZERO)
                    .totalCoinAmount(BigDecimal.ZERO)
                    .totalCreatorShareAmount(BigDecimal.ZERO)
                    .totalNetRevenue(BigDecimal.ZERO)
                    .build();
        }

        return ContentRevenueOverviewDto.builder()
                .totalGrossRevenue(data.grossRevenue())
                .totalVatAmount(data.vatAmount())
                .totalCoinAmount(data.coinAmount())
                .totalCreatorShareAmount(data.creatorShareAmount())
                .totalNetRevenue(data.netRevenue())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContentRevenueDetailDto> getContentDetails(LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);

        long daysBetween = Duration.between(startTime, endTime).toDays();

        String dateFormatPattern;
        String groupUnit;

        if (daysBetween < 7) {
            dateFormatPattern = "YYYY-MM-DD HH24:00";
            groupUnit = "HOUR";
        } else if (daysBetween < 30) {
            dateFormatPattern = "YYYY-MM-DD";
            groupUnit = "DAY";
        } else if (startTime.plusMonths(12).isAfter(endTime)) { // Dưới 12 tháng
            dateFormatPattern = "YYYY-MM";
            groupUnit = "MONTH";
        } else { // Từ 12 tháng trở lên
            dateFormatPattern = "YYYY";
            groupUnit = "YEAR";
        }

        String completedStatus = OrderStatus.COMPLETED.name();
        List<ContentRevenueStatisticData> rawDataList = orderRepository.getContentGroupedStatistics(
                completedStatus, startTime, endTime, dateFormatPattern
        );

        return rawDataList.stream()
                .map(data -> ContentRevenueDetailDto.builder()
                        .period(data.period())
                        .grossRevenue(data.grossRevenue())
                        .vatAmount(data.vatAmount())
                        .coinAmount(data.coinAmount())
                        .creatorShareAmount(data.creatorShareAmount())
                        .netRevenue(data.netRevenue())
                        .groupUnit(groupUnit)
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportContentExcel(LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);
        ContentRevenueOverviewDto overview = getContentOverview(startTime, endTime);
        List<OrderDetailStatisticProjection> details = orderRepository.getContentOrderDetails(
                OrderStatus.COMPLETED.name(), startTime, endTime
        );
        return buildExcelWorkbook("BÁO CÁO DOANH THU COMBO & EPISODE", overviewDataToRows(overview), details);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportContentExcelByItemId(String itemId, LocalDateTime startTime, LocalDateTime endTime) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId không được để trống!");
        }
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("startTime phải nhỏ hơn hoặc bằng endTime!");
        }

        List<OrderDetailStatisticProjection> details = orderRepository.getContentOrderDetailsByItemId(
                itemId, OrderStatus.COMPLETED.name(), startTime, endTime
        );

        String title = "BÁO CÁO ĐƠN HÀNG NỘI DUNG (ITEM ID: " + itemId + ")";
        Object[][] overviewRows = new Object[][]{
                {"Mã vật phẩm (Item ID)", itemId},
                {"Tổng số đơn hoàn tất", details.size()}
        };

        return buildExcelWorkbook(title, overviewRows, details);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionRevenueOverviewDto getSubscriptionOverview(LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);

        String completedStatus = OrderStatus.COMPLETED.name();
        SubscriptionStatisticData data = orderRepository.getSubscriptionOverviewStatistic(
                completedStatus, SUBSCRIPTION_ITEM_TYPE, startTime, endTime
        );

        if (data == null) {
            return SubscriptionRevenueOverviewDto.builder()
                    .totalGrossRevenue(BigDecimal.ZERO)
                    .totalVatAmount(BigDecimal.ZERO)
                    .totalNetRevenue(BigDecimal.ZERO)
                    .build();
        }

        return SubscriptionRevenueOverviewDto.builder()
                .totalGrossRevenue(data.grossRevenue())
                .totalVatAmount(data.vatAmount())
                .totalNetRevenue(data.netRevenue())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionRevenueDetailDto> getSubscriptionDetails(LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);

        long daysBetween = Duration.between(startTime, endTime).toDays();

        String dateFormatPattern;
        String groupUnit;

        if (daysBetween < 7) {
            dateFormatPattern = "YYYY-MM-DD HH24:00";
            groupUnit = "HOUR";
        } else if (daysBetween < 30) {
            dateFormatPattern = "YYYY-MM-DD";
            groupUnit = "DAY";
        } else if (startTime.plusMonths(12).isAfter(endTime)) { // Dưới 12 tháng
            dateFormatPattern = "YYYY-MM";
            groupUnit = "MONTH";
        } else { // Từ 12 tháng trở lên
            dateFormatPattern = "YYYY";
            groupUnit = "YEAR";
        }

        String completedStatus = OrderStatus.COMPLETED.name();
        List<SubscriptionStatisticData> rawDataList = orderRepository.getSubscriptionGroupedStatistics(
                completedStatus, SUBSCRIPTION_ITEM_TYPE, startTime, endTime, dateFormatPattern
        );

        return rawDataList.stream()
                .map(data -> SubscriptionRevenueDetailDto.builder()
                        .period(data.period())
                        .grossRevenue(data.grossRevenue())
                        .vatAmount(data.vatAmount())
                        .netRevenue(data.netRevenue())
                        .groupUnit(groupUnit)
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportSubscriptionExcel(LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);
        SubscriptionRevenueOverviewDto overview = getSubscriptionOverview(startTime, endTime);
        List<OrderDetailStatisticProjection> details = orderRepository.getSubscriptionOrderDetails(
                OrderStatus.COMPLETED.name(), SUBSCRIPTION_ITEM_TYPE, startTime, endTime
        );
        return buildExcelWorkbook("BÁO CÁO DOANH THU PREMIUM", overviewDataToRows(overview), details);
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime và endTime không được để trống!");
        }
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("startTime phải nhỏ hơn hoặc bằng endTime!");
        }
    }

    private Object[][] overviewDataToRows(Object overviewDto) {
        if (overviewDto instanceof CampaignRevenueOverviewDto dto) {
            return new Object[][]{
                    {"Tổng doanh thu (Gross Revenue)", dto.getTotalGrossRevenue()},
                    {"Tổng thuế VAT", dto.getTotalVatAmount()},
                    {"Doanh thu thuần (Net Revenue)", dto.getTotalNetRevenue()}
            };
        } else if (overviewDto instanceof ContentRevenueOverviewDto dto) {
            return new Object[][]{
                    {"Tổng doanh thu (Gross Revenue)", dto.getTotalGrossRevenue()},
                    {"Tổng thuế VAT", dto.getTotalVatAmount()},
                    {"Tổng coin sử dụng", dto.getTotalCoinAmount()},
                    {"Tổng chi trả Creator Share", dto.getTotalCreatorShareAmount()},
                    {"Doanh thu thuần (Net Revenue)", dto.getTotalNetRevenue()}
            };
        } else if (overviewDto instanceof SubscriptionRevenueOverviewDto dto) {
            return new Object[][]{
                    {"Tổng doanh thu (Gross Revenue)", dto.getTotalGrossRevenue()},
                    {"Tổng thuế VAT", dto.getTotalVatAmount()},
                    {"Doanh thu thuần (Net Revenue)", dto.getTotalNetRevenue()}
            };
        }
        return new Object[][]{};
    }

    private byte[] buildExcelWorkbook(String reportTitle, Object[][] overviewRows, List<OrderDetailStatisticProjection> details) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Formatting Styles
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // -----------------------------------------------------------------
            // TAB 1: TỔNG QUAN (OVERVIEW)
            // -----------------------------------------------------------------
            Sheet overviewSheet = workbook.createSheet("Tổng quan");

            // Title Row
            Row titleRow = overviewSheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(reportTitle);
            titleCell.setCellStyle(titleStyle);

            // Table Header
            Row ovHeaderRow = overviewSheet.createRow(2);
            Cell h1 = ovHeaderRow.createCell(0);
            h1.setCellValue("Chỉ số tổng quan");
            h1.setCellStyle(headerStyle);

            Cell h2 = ovHeaderRow.createCell(1);
            h2.setCellValue("Giá trị (VNĐ / Xu)");
            h2.setCellStyle(headerStyle);

            int rowIdx = 3;
            for (Object[] rowData : overviewRows) {
                Row row = overviewSheet.createRow(rowIdx++);
                Cell cLabel = row.createCell(0);
                cLabel.setCellValue((String) rowData[0]);
                cLabel.setCellStyle(dataStyle);

                Cell cValue = row.createCell(1);
                if (rowData[1] instanceof BigDecimal val) {
                    cValue.setCellValue(val.doubleValue());
                    cValue.setCellStyle(currencyStyle);
                } else if (rowData[1] instanceof Number val) {
                    cValue.setCellValue(val.doubleValue());
                    cValue.setCellStyle(currencyStyle);
                } else {
                    cValue.setCellValue(String.valueOf(rowData[1]));
                    cValue.setCellStyle(dataStyle);
                }
            }

            overviewSheet.autoSizeColumn(0);
            overviewSheet.autoSizeColumn(1);

            // -----------------------------------------------------------------
            // TAB 2: CHI TIẾT (DETAILS)
            // -----------------------------------------------------------------
            Sheet detailSheet = workbook.createSheet("Chi tiết");

            String[] headers = {
                    "Mã Đơn Hàng", "Tổng tiền trước thuế", "Số xu sử dụng", "Tiền thuế VAT",
                    "Tiền chia sẻ Creator", "Mô tả", "Doanh thu thực nhận (Fiat)", "Trạng thái",
                    "Thời gian tạo", "Thời gian cập nhật", "Loại sản phẩm", "Mã sản phẩm",
                    "Tên sản phẩm", "Mã người mua", "Email người mua", "Tên người mua"
            };

            Row detailHeaderRow = detailSheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = detailHeaderRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int detailRowIdx = 1;
            for (OrderDetailStatisticProjection item : details) {
                Row row = detailSheet.createRow(detailRowIdx++);

                // orderId
                createCell(row, 0, item.getOrderId(), dataStyle);
                // total_amount
                createCurrencyCell(row, 1, item.getTotalAmount(), currencyStyle);
                // coin_amount
                createCurrencyCell(row, 2, item.getCoinAmount() != null ? BigDecimal.valueOf(item.getCoinAmount()) : BigDecimal.ZERO, currencyStyle);
                // vat_amount
                createCurrencyCell(row, 3, item.getVatAmount(), currencyStyle);
                // share_amount
                createCurrencyCell(row, 4, item.getShareAmount(), currencyStyle);
                // description
                createCell(row, 5, item.getDescription(), dataStyle);
                // fiat_amount
                createCurrencyCell(row, 6, item.getFiatAmount(), currencyStyle);
                // status
                createCell(row, 7, item.getStatus(), dataStyle);
                // created_at
                createCell(row, 8, item.getCreatedAt() != null ? item.getCreatedAt().format(dateFormatter) : "", dataStyle);
                // updatedAt
                createCell(row, 9, item.getUpdatedAt() != null ? item.getUpdatedAt().format(dateFormatter) : "", dataStyle);
                // item_type (Diễn giải tiếng Việt kèm mã)
                createCell(row, 10, formatItemTypeDisplay(item.getItemType()), dataStyle);
                // item_id
                createCell(row, 11, item.getItemId(), dataStyle);
                // itemName
                createCell(row, 12, item.getItemName(), dataStyle);
                // account_id
                createCell(row, 13, item.getAccountId(), dataStyle);
                // email
                createCell(row, 14, item.getEmail(), dataStyle);
                // fullName
                createCell(row, 15, item.getFullName(), dataStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                detailSheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi tạo file Excel thống kê: " + e.getMessage(), e);
        }
    }

    private byte[] buildAllExcelWorkbook(
            OrderStatisticData overview,
            List<OrderDetailStatisticProjection> campaignDetails,
            List<OrderDetailStatisticProjection> contentDetails,
            List<OrderDetailStatisticProjection> subscriptionDetails
    ) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            // -----------------------------------------------------------------
            // TAB 1: TỔNG QUAN HỆ THỐNG
            // -----------------------------------------------------------------
            Sheet overviewSheet = workbook.createSheet("Tổng quan hệ thống");
            Row titleRow = overviewSheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO THỐNG KÊ TÀI CHÍNH TỔNG QUAN");
            titleCell.setCellStyle(titleStyle);

            Row ovHeaderRow = overviewSheet.createRow(2);
            Cell h1 = ovHeaderRow.createCell(0);
            h1.setCellValue("Chỉ số tài chính");
            h1.setCellStyle(headerStyle);

            Cell h2 = ovHeaderRow.createCell(1);
            h2.setCellValue("Giá trị (VNĐ / Xu)");
            h2.setCellStyle(headerStyle);

            Object[][] overallRows = new Object[][]{
                    {"Tổng GMV (Tổng giá trị giao dịch)", overview != null ? overview.gmv() : BigDecimal.ZERO},
                    {"Doanh thu thuần (Net Revenue)", overview != null ? overview.netRevenue() : BigDecimal.ZERO},
                    {"Tổng thuế VAT", overview != null ? overview.vatAmount() : BigDecimal.ZERO},
                    {"Tổng số Coin sử dụng", overview != null ? overview.totalCoin() : 0L}
            };

            int rowIdx = 3;
            for (Object[] rowData : overallRows) {
                Row row = overviewSheet.createRow(rowIdx++);
                Cell cLabel = row.createCell(0);
                cLabel.setCellValue((String) rowData[0]);
                cLabel.setCellStyle(dataStyle);

                Cell cValue = row.createCell(1);
                if (rowData[1] instanceof BigDecimal val) {
                    cValue.setCellValue(val.doubleValue());
                    cValue.setCellStyle(currencyStyle);
                } else if (rowData[1] instanceof Number val) {
                    cValue.setCellValue(val.doubleValue());
                    cValue.setCellStyle(currencyStyle);
                } else {
                    cValue.setCellValue(String.valueOf(rowData[1]));
                    cValue.setCellStyle(dataStyle);
                }
            }
            overviewSheet.autoSizeColumn(0);
            overviewSheet.autoSizeColumn(1);

            // -----------------------------------------------------------------
            // TAB 2: CAMPAIGN
            // -----------------------------------------------------------------
            populateDetailSheet(workbook, "Campaign", campaignDetails, headerStyle, dataStyle, currencyStyle);

            // -----------------------------------------------------------------
            // TAB 3: COMBO & EPISODE
            // -----------------------------------------------------------------
            populateDetailSheet(workbook, "Combo & Episode", contentDetails, headerStyle, dataStyle, currencyStyle);

            // -----------------------------------------------------------------
            // TAB 4: PREMIUM
            // -----------------------------------------------------------------
            populateDetailSheet(workbook, "Premium", subscriptionDetails, headerStyle, dataStyle, currencyStyle);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi tạo file Excel thống kê tổng quan: " + e.getMessage(), e);
        }
    }

    private String formatItemTypeDisplay(String itemType) {
        if (itemType == null) return "";
        return switch (itemType) {
            case "ENGAGEMENT" -> "ENGAGEMENT - Dịch vụ tăng lượt hiển thị";
            case "PREMIUM", "SUBSCRIPTION" -> "PREMIUM - Gói Nâng cấp tài khoản";
            case "EPISODE" -> "EPISODE - Tập series";
            case "COMBO" -> "COMBO - Combo các tập thuộc series";
            default -> itemType;
        };
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void createCurrencyCell(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value.doubleValue() : 0.0);
        cell.setCellStyle(style);
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Arial");
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Arial");
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.BLUE1.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Arial");
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private void populateDetailSheet(
            Workbook workbook,
            String sheetName,
            List<OrderDetailStatisticProjection> details,
            CellStyle headerStyle,
            CellStyle dataStyle,
            CellStyle currencyStyle
    ) {
        Sheet sheet = workbook.createSheet(sheetName);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String[] headers = {
                "Mã Đơn Hàng", "Tổng tiền trước thuế", "Số xu sử dụng", "Tiền thuế VAT",
                "Tiền chia sẻ Creator", "Mô tả", "Doanh thu thực nhận (Fiat)", "Trạng thái",
                "Thời gian tạo", "Thời gian cập nhật", "Loại sản phẩm", "Mã sản phẩm",
                "Tên sản phẩm", "Mã người mua", "Email người mua", "Tên người mua"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (OrderDetailStatisticProjection item : details) {
            Row row = sheet.createRow(rowIdx++);
            createCell(row, 0, item.getOrderId(), dataStyle);
            createCurrencyCell(row, 1, item.getTotalAmount(), currencyStyle);
            createCurrencyCell(row, 2, item.getCoinAmount() != null ? BigDecimal.valueOf(item.getCoinAmount()) : BigDecimal.ZERO, currencyStyle);
            createCurrencyCell(row, 3, item.getVatAmount(), currencyStyle);
            createCurrencyCell(row, 4, item.getShareAmount(), currencyStyle);
            createCell(row, 5, item.getDescription(), dataStyle);
            createCurrencyCell(row, 6, item.getFiatAmount(), currencyStyle);
            createCell(row, 7, item.getStatus(), dataStyle);
            createCell(row, 8, item.getCreatedAt() != null ? item.getCreatedAt().format(dateFormatter) : "", dataStyle);
            createCell(row, 9, item.getUpdatedAt() != null ? item.getUpdatedAt().format(dateFormatter) : "", dataStyle);
            createCell(row, 10, formatItemTypeDisplay(item.getItemType()), dataStyle);
            createCell(row, 11, item.getItemId(), dataStyle);
            createCell(row, 12, item.getItemName(), dataStyle);
            createCell(row, 13, item.getAccountId(), dataStyle);
            createCell(row, 14, item.getEmail(), dataStyle);
            createCell(row, 15, item.getFullName(), dataStyle);
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}