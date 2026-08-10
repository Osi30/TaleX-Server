package com.talex.server.schedulers;

import com.talex.server.entities.transaction.Invoice;
import com.talex.server.enums.transaction.InvoiceStatus;
import com.talex.server.repositories.transaction.InvoiceRepository;
import com.talex.server.services.invoice.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceStatusPollingScheduler {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;

    // TẠM COMMENT: xuất hóa đơn điện tử qua SePay đang lỗi phía nhà cung cấp (matbao) — xem
    // giải thích ở OrderCompletionServiceImpl.complete(). Tắt @Scheduled để không tiếp tục
    // gọi API SePay vô ích trong lúc chờ xác nhận.
    // @Scheduled(
    //         fixedDelayString = "${payment.invoice.polling-fixed-delay-ms:60000}",
    //         initialDelayString = "${payment.invoice.polling-initial-delay-ms:60000}")
    public void pollPendingInvoices() {
        List<Invoice> pendingInvoices = invoiceRepository.findTop50ByStatusOrderByCreatedAtAsc(InvoiceStatus.PENDING);

        for (Invoice invoice : pendingInvoices) {
            try {
                // Chưa có tracking_code = chưa từng gửi request tạo hóa đơn lên SePay.
                if (invoice.getTrackingCode() == null) {
                    invoiceService.submitPendingInvoice(invoice);
                } else {
                    invoiceService.pollPendingInvoice(invoice);
                }
            } catch (RuntimeException exception) {
                log.warn("Failed to process invoice {}", invoice.getInvoiceId(), exception);
            }
        }
    }
}
