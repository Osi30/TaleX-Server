package com.talex.server.services.payment.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.responses.payment.AdminOrderDetailDto;
import com.talex.server.dtos.responses.payment.AdminOrderListItemDto;
import com.talex.server.dtos.responses.payment.AdminOrderStatsDto;
import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.exceptions.codes.payment.PaymentErrorCode;
import com.talex.server.exceptions.details.payment.PaymentException;
import com.talex.server.repositories.transaction.OrderRepository;
import com.talex.server.services.payment.AdminOrderQueryService;
import com.talex.server.specifications.OrderAdminSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminOrderQueryServiceImpl implements AdminOrderQueryService {

    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<AdminOrderListItemDto> search(
            OrderStatus status, String itemType, LocalDateTime createdAtFrom, LocalDateTime createdAtTo,
            String keyword, int page, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> result = orderRepository.findAll(
                OrderAdminSpec.searchForAdmin(status, itemType, createdAtFrom, createdAtTo, keyword), pageable);
        return toListPageResponse(result);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderDetailDto getDetail(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.ORDER_NOT_FOUND));
        return toDetailDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderStatsDto getStats(LocalDateTime from, LocalDateTime to) {
        Map<String, Long> countByStatus = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            countByStatus.put(status.name(), 0L);
        }
        long total = 0;
        for (Object[] row : orderRepository.countByStatusGrouped(from, to)) {
            OrderStatus status = (OrderStatus) row[0];
            long count = (Long) row[1];
            countByStatus.put(status.name(), count);
            total += count;
        }

        List<AdminOrderStatsDto.ItemTypeRevenue> revenueByItemType = orderRepository
                .sumRevenueByItemTypeGrouped(OrderStatus.COMPLETED, from, to)
                .stream()
                .map(row -> AdminOrderStatsDto.ItemTypeRevenue.builder()
                        .itemType((String) row[0])
                        .totalRevenue((BigDecimal) row[1])
                        .orderCount((Long) row[2])
                        .build())
                .toList();

        long cancelledCount = countByStatus.getOrDefault(OrderStatus.CANCELLED.name(), 0L);
        long expiredCount = countByStatus.getOrDefault(OrderStatus.OUT_OF_TIME.name(), 0L);

        return AdminOrderStatsDto.builder()
                .totalOrders(total)
                .countByStatus(countByStatus)
                .revenueByItemType(revenueByItemType)
                .cancelledRatePercent(total == 0 ? 0.0 : cancelledCount * 100.0 / total)
                .expiredRatePercent(total == 0 ? 0.0 : expiredCount * 100.0 / total)
                .build();
    }

    @Override
    public AdminOrderDetailDto toDetailDto(Order order) {
        return AdminOrderDetailDto.builder()
                .orderId(order.getOrderId())
                .paymentCode(order.getPaymentCode())
                .status(order.getStatus())
                .itemType(order.getItemType())
                .itemId(order.getItemId())
                .totalAmount(order.getTotalAmount())
                .coinAmount(order.getCoinAmount())
                .fiatAmount(order.getFiatAmount())
                .campaignWalletAmount(order.getCampaignWalletAmount())
                .overpaidAmount(order.getOverpaidAmount())
                .vatRate(order.getVatRate())
                .vatAmount(order.getVatAmount())
                .metadata(order.getMetadata())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .expiresAt(order.getExpiresAt())
                .buyerAccountId(order.getAccount() != null ? order.getAccount().getAccountId() : null)
                .buyerUsername(order.getAccount() != null ? order.getAccount().getUsername() : null)
                .buyerEmail(order.getAccount() != null ? order.getAccount().getEmail() : null)
                .build();
    }

    private BasePageResponse<AdminOrderListItemDto> toListPageResponse(Page<Order> page) {
        List<AdminOrderListItemDto> content = page.getContent().stream()
                .map(this::toListItemDto)
                .toList();
        return BasePageResponse.<AdminOrderListItemDto>builder()
                .content(content)
                .pageNumber(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }

    private AdminOrderListItemDto toListItemDto(Order order) {
        return AdminOrderListItemDto.builder()
                .orderId(order.getOrderId())
                .paymentCode(order.getPaymentCode())
                .status(order.getStatus())
                .itemType(order.getItemType())
                .itemId(order.getItemId())
                .totalAmount(order.getTotalAmount())
                .coinAmount(order.getCoinAmount())
                .fiatAmount(order.getFiatAmount())
                .campaignWalletAmount(order.getCampaignWalletAmount())
                .vatAmount(order.getVatAmount())
                .createdAt(order.getCreatedAt())
                .expiresAt(order.getExpiresAt())
                .buyerUsername(order.getAccount() != null ? order.getAccount().getUsername() : null)
                .buyerEmail(order.getAccount() != null ? order.getAccount().getEmail() : null)
                .build();
    }
}
