package com.talex.server.services.payment.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.responses.payment.OrderHistoryItemDto;
import com.talex.server.entities.series.ComboEpisode;
import com.talex.server.entities.series.Episode;
import com.talex.server.entities.transaction.Invoice;
import com.talex.server.entities.transaction.Order;
import com.talex.server.entities.transaction.Transaction;
import com.talex.server.enums.transaction.ReferenceType;
import com.talex.server.repositories.series.ComboEpisodeRepository;
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.repositories.transaction.InvoiceRepository;
import com.talex.server.repositories.transaction.OrderRepository;
import com.talex.server.repositories.transaction.TransactionRepository;
import com.talex.server.services.payment.OrderHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderHistoryServiceImpl implements OrderHistoryService {

    private static final String UNKNOWN_ITEM_TITLE = "Nội dung đã gỡ";
    private static final List<String> CONTENT_ITEM_TYPES = List.of(
            EpisodeOrderFulfillmentService.ITEM_TYPE, ComboOrderFulfillmentService.ITEM_TYPE);

    private final OrderRepository orderRepository;
    private final EpisodeRepository episodeRepository;
    private final ComboEpisodeRepository comboEpisodeRepository;
    private final TransactionRepository transactionRepository;
    private final InvoiceRepository invoiceRepository;

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<OrderHistoryItemDto> getContentPurchaseHistory(UUID accountId, Pageable pageable) {
        Page<Order> orderPage = orderRepository
                .findByAccount_AccountIdAndItemTypeInOrderByCreatedAtDesc(accountId, CONTENT_ITEM_TYPES, pageable);
        List<Order> orders = orderPage.getContent();

        Map<String, String> titleByEpisodeId = resolveTitles(orders,
                EpisodeOrderFulfillmentService.ITEM_TYPE, episodeRepository::findAllById,
                Episode::getEpisodeId, Episode::getTitle);
        Map<String, String> titleByComboId = resolveTitles(orders,
                ComboOrderFulfillmentService.ITEM_TYPE, comboEpisodeRepository::findAllById,
                ComboEpisode::getComboId, ComboEpisode::getTitle);

        List<String> orderIds = orders.stream().map(Order::getOrderId).toList();
        Map<String, Transaction> transactionByOrderId = transactionRepository
                .findByReferenceTypeAndReferenceIdIn(ReferenceType.ORDER, orderIds).stream()
                .collect(Collectors.toMap(Transaction::getReferenceId, Function.identity(), (a, b) -> a));

        List<String> transactionIds = transactionByOrderId.values().stream()
                .map(Transaction::getTransactionId).toList();
        Map<String, Invoice> invoiceByTransactionId = invoiceRepository.findByTransaction_TransactionIdIn(transactionIds)
                .stream()
                .collect(Collectors.toMap(invoice -> invoice.getTransaction().getTransactionId(),
                        Function.identity(), (a, b) -> a));

        List<OrderHistoryItemDto> items = orders.stream()
                .map(order -> toHistoryItem(order, titleByEpisodeId, titleByComboId,
                        transactionByOrderId, invoiceByTransactionId))
                .toList();

        return BasePageResponse.<OrderHistoryItemDto>builder()
                .content(items)
                .pageNumber(orderPage.getNumber() + 1)
                .pageSize(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .isFirst(orderPage.isFirst())
                .isLast(orderPage.isLast())
                .build();
    }

    private <T> Map<String, String> resolveTitles(
            List<Order> orders, String itemType, Function<List<String>, List<T>> fetchByIds,
            Function<T, String> idExtractor, Function<T, String> titleExtractor) {
        List<String> itemIds = orders.stream()
                .filter(order -> itemType.equals(order.getItemType()))
                .map(Order::getItemId)
                .toList();
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        return fetchByIds.apply(itemIds).stream()
                .collect(Collectors.toMap(idExtractor, titleExtractor, (a, b) -> a));
    }

    private OrderHistoryItemDto toHistoryItem(
            Order order, Map<String, String> titleByEpisodeId, Map<String, String> titleByComboId,
            Map<String, Transaction> transactionByOrderId, Map<String, Invoice> invoiceByTransactionId) {
        String itemTitle = EpisodeOrderFulfillmentService.ITEM_TYPE.equals(order.getItemType())
                ? titleByEpisodeId.getOrDefault(order.getItemId(), UNKNOWN_ITEM_TITLE)
                : titleByComboId.getOrDefault(order.getItemId(), UNKNOWN_ITEM_TITLE);

        Transaction transaction = transactionByOrderId.get(order.getOrderId());
        Invoice invoice = transaction != null ? invoiceByTransactionId.get(transaction.getTransactionId()) : null;

        return OrderHistoryItemDto.builder()
                .orderId(order.getOrderId())
                .itemType(order.getItemType())
                .itemTitle(itemTitle)
                .totalAmount(order.getTotalAmount())
                .paymentMethod(transaction != null ? transaction.getPaymentMethod() : null)
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .invoiceUrl(invoice != null ? invoice.getInvoiceUrl() : null)
                .build();
    }
}
