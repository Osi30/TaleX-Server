package com.talex.server.services.invoice.impls;

import com.talex.server.dtos.requests.invoice.SePayCreateInvoiceRequestDto;
import com.talex.server.dtos.requests.invoice.SePayInvoiceBuyerDto;
import com.talex.server.dtos.requests.invoice.SePayInvoiceItemDto;
import com.talex.server.dtos.responses.invoice.SePayCreateInvoiceResponseDataDto;
import com.talex.server.dtos.responses.invoice.SePayInvoiceStatusDataDto;
import com.talex.server.dtos.responses.invoice.SePayProviderAccountDetailDto;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.transaction.Invoice;
import com.talex.server.entities.transaction.Order;
import com.talex.server.entities.transaction.Transaction;
import com.talex.server.enums.transaction.InvoiceStatus;
import com.talex.server.enums.transaction.PaymentMethod;
import com.talex.server.enums.transaction.ReferenceType;
import com.talex.server.repositories.transaction.InvoiceRepository;
import com.talex.server.repositories.transaction.OrderRepository;
import com.talex.server.services.auth.EmailService;
import com.talex.server.services.invoice.IInvoiceService;
import com.talex.server.services.invoice.SePayEInvoiceClient;
import com.talex.server.services.payment.impls.ComboOrderFulfillmentService;
import com.talex.server.services.payment.impls.EngagementOrderFulfillmentService;
import com.talex.server.services.payment.impls.EpisodeOrderFulfillmentService;
import com.talex.server.services.payment.impls.SubscriptionOrderFulfillmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements IInvoiceService {

    private static final DateTimeFormatter ISSUED_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // SePay validate issued_date theo giờ Việt Nam — container chạy giờ UTC (không set TZ)
    // nên phải quy đổi tường minh, nếu không issued_date sẽ bị coi là "ở quá khứ".
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String CURRENCY_VND = "VND";
    private static final String PAYMENT_METHOD_BANK_TRANSFER = "CK";
    private static final int LINE_TYPE_PRODUCT = 1;

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final SePayEInvoiceClient eInvoiceClient;
    private final EmailService emailService;
    private final RestTemplate restTemplate;

    @Value("${payment.invoice.max-poll-attempts:20}")
    private int maxPollAttempts;

    // Chạy trong CÙNG transaction với complete() — chỉ ghi 1 dòng PENDING, không gọi
    // API SePay ở đây (việc đó để scheduler làm độc lập, tránh mọi vấn đề về thời điểm
    // commit/visibility khi gọi xen giữa transaction thanh toán).
    @Override
    @Transactional
    public void markPendingInvoice(Order order, Transaction transaction) {
        if (transaction.getPaymentMethod() != PaymentMethod.SEPAY) {
            return;
        }

        Invoice invoice = Invoice.builder()
                .transaction(transaction)
                .status(InvoiceStatus.PENDING)
                .build();
        invoiceRepository.save(invoice);
    }

    @Override
    @Transactional
    public void submitPendingInvoice(Invoice invoice) {
        try {
            Order order = resolveOrder(invoice)
                    .orElseThrow(() -> new IllegalStateException("Order not found for invoice " + invoice.getInvoiceId()));
            SePayProviderAccountDetailDto providerAccount = eInvoiceClient.resolveDefaultProviderAccount();
            SePayCreateInvoiceRequestDto request = buildCreateInvoiceRequest(order, invoice.getTransaction(), providerAccount);
            SePayCreateInvoiceResponseDataDto response = eInvoiceClient.createInvoice(request);

            if (response == null || response.getTrackingCode() == null) {
                throw new IllegalStateException("SePay eInvoice: empty tracking_code on create response");
            }

            invoice.setTrackingCode(response.getTrackingCode());
            invoiceRepository.save(invoice);
        } catch (RuntimeException exception) {
            // Lỗi mạng/timeout tạm thời (SePay chậm, mất kết nối...) không được phép làm
            // mất hóa đơn vĩnh viễn — giữ PENDING và thử lại như pollPendingInvoice, chỉ
            // đánh FAILED sau khi vượt quá số lần thử cho phép.
            log.warn("Failed to submit SePay eInvoice for invoice {}, will retry", invoice.getInvoiceId(), exception);
            registerPollAttempt(invoice, exception.getMessage());
        }
    }

    @Override
    @Transactional
    public void pollPendingInvoice(Invoice invoice) {
        try {
            SePayInvoiceStatusDataDto status = eInvoiceClient.checkCreationStatus(invoice.getTrackingCode());
            if (status == null || status.getStatus() == null) {
                registerPollAttempt(invoice, null);
                return;
            }

            if ("Success".equalsIgnoreCase(status.getStatus())) {
                completeInvoice(invoice, status);
            } else if ("Failed".equalsIgnoreCase(status.getStatus())) {
                invoice.setStatus(InvoiceStatus.FAILED);
                invoice.setFailureReason(status.getMessage());
                invoiceRepository.save(invoice);
            } else {
                registerPollAttempt(invoice, status.getMessage());
            }
        } catch (RuntimeException exception) {
            log.warn("SePay eInvoice status check failed for tracking_code={}", invoice.getTrackingCode(), exception);
            registerPollAttempt(invoice, exception.getMessage());
        }
    }

    private void registerPollAttempt(Invoice invoice, String lastMessage) {
        int attempts = invoice.getPollAttempts() + 1;
        invoice.setPollAttempts(attempts);
        if (attempts >= maxPollAttempts) {
            invoice.setStatus(InvoiceStatus.FAILED);
            invoice.setFailureReason(lastMessage != null
                    ? lastMessage
                    : "Hết số lần thử tra cứu trạng thái xuất hóa đơn");
        }
        invoiceRepository.save(invoice);
    }

    private void completeInvoice(Invoice invoice, SePayInvoiceStatusDataDto status) {
        invoice.setStatus(InvoiceStatus.COMPLETED);
        if (status.getInvoice() != null) {
            invoice.setInvoiceNumber(status.getInvoice().getInvoiceNumber());
            invoice.setInvoiceUrl(status.getInvoice().getPdfUrl());
        }
        invoice.setReservationCode(status.getReferenceCode());
        invoiceRepository.save(invoice);

        sendConfirmationEmail(invoice);
    }

    private void sendConfirmationEmail(Invoice invoice) {
        if (invoice.getInvoiceUrl() == null) {
            return;
        }
        String buyerEmail = resolveBuyerEmail(invoice);
        if (buyerEmail == null) {
            log.warn("SePay eInvoice {} completed but no buyer email to notify", invoice.getInvoiceId());
            return;
        }
        byte[] pdfBytes = downloadInvoicePdf(invoice.getInvoiceUrl());
        emailService.sendInvoiceEmailAsync(buyerEmail, invoice.getInvoiceUrl(), pdfBytes);
    }

    // Tải file PDF hóa đơn về để đính kèm email — không được để lỗi tải file chặn việc
    // gửi email, nếu tải hỏng thì vẫn gửi email kèm link xem hóa đơn như bình thường.
    private byte[] downloadInvoicePdf(String invoicePdfUrl) {
        try {
            return restTemplate.getForObject(invoicePdfUrl, byte[].class);
        } catch (RuntimeException exception) {
            log.warn("Failed to download invoice PDF from {}", invoicePdfUrl, exception);
            return null;
        }
    }

    private String resolveBuyerEmail(Invoice invoice) {
        return resolveOrder(invoice).map(Order::getAccount).map(Account::getEmail).orElse(null);
    }

    // Invoice -> Transaction -> Order.orderId (referenceId, không phải FK thật) -> Order.
    private Optional<Order> resolveOrder(Invoice invoice) {
        Transaction transaction = invoice.getTransaction();
        if (transaction == null || transaction.getReferenceType() != ReferenceType.ORDER
                || transaction.getReferenceId() == null) {
            return Optional.empty();
        }
        return orderRepository.findById(transaction.getReferenceId());
    }

    private SePayCreateInvoiceRequestDto buildCreateInvoiceRequest(
            Order order, Transaction transaction, SePayProviderAccountDetailDto providerAccount) {
        var template = providerAccount.getTemplates().get(0);
        Account account = order.getAccount();
        long amount = transaction.getPaidAmount().setScale(0, RoundingMode.HALF_UP).longValueExact();

        SePayInvoiceItemDto item = SePayInvoiceItemDto.builder()
                .lineNumber(1)
                .lineType(LINE_TYPE_PRODUCT)
                .itemCode(order.getItemType())
                .itemName(resolveItemName(order.getItemType()))
                .unit("Gói")
                .quantity(BigDecimal.ONE)
                .unitPrice(amount)
                .build();

        SePayInvoiceBuyerDto buyer = SePayInvoiceBuyerDto.builder()
                .name(account.getFullName() != null ? account.getFullName() : account.getUsername())
                .email(account.getEmail())
                .phone(account.getPhone())
                .build();

        return SePayCreateInvoiceRequestDto.builder()
                .templateCode(template.getTemplateCode())
                .invoiceSeries(template.getInvoiceSeries())
                .issuedDate(LocalDateTime.now(VIETNAM_ZONE).format(ISSUED_DATE_FORMAT))
                .currency(CURRENCY_VND)
                .providerAccountId(providerAccount.getId())
                .referenceCode(order.getOrderId())
                .paymentMethod(PAYMENT_METHOD_BANK_TRANSFER)
                .isDraft(false)
                .buyer(buyer)
                .items(List.of(item))
                .totalAmount(amount)
                .build();
    }

    private String resolveItemName(String itemType) {
        if (itemType == null) {
            return "Đơn hàng TaleX";
        }
        return switch (itemType) {
            case SubscriptionOrderFulfillmentService.ITEM_TYPE -> "Gói Premium TaleX";
            case EngagementOrderFulfillmentService.ITEM_TYPE -> "Gói tương tác TaleX";
            case EpisodeOrderFulfillmentService.ITEM_TYPE -> "Mua tập nội dung TaleX";
            case ComboOrderFulfillmentService.ITEM_TYPE -> "Mua combo nội dung TaleX";
            default -> "Đơn hàng TaleX";
        };
    }
}
