package com.talex.server.schedulers;

import com.talex.server.entities.transaction.Invoice;
import com.talex.server.enums.transaction.InvoiceStatus;
import com.talex.server.repositories.transaction.InvoiceRepository;
import com.talex.server.services.invoice.IInvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceStatusPollingScheduler {

    private final InvoiceRepository invoiceRepository;
    private final IInvoiceService invoiceService;

    @Scheduled(
            fixedDelayString = "${payment.invoice.polling-fixed-delay-ms:60000}",
            initialDelayString = "${payment.invoice.polling-initial-delay-ms:60000}")
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
