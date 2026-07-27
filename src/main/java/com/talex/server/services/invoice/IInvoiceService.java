package com.talex.server.services.invoice;

import com.talex.server.entities.transaction.Invoice;
import com.talex.server.entities.transaction.Order;
import com.talex.server.entities.transaction.Transaction;

public interface IInvoiceService {
    /**
     * Gọi ngay trong CÙNG transaction với việc hoàn tất Order — chỉ ghi 1 dòng Invoice
     * PENDING (chưa gọi SePay), nên không có rủi ro FK do Order/Transaction chưa commit.
     * Chỉ đánh dấu cho giao dịch bằng tiền thật (SEPAY) — Coin là tiền ảo kiếm được từ
     * nhiệm vụ/quảng cáo, không phải tiền nạp nên không phát sinh nghĩa vụ xuất hóa đơn.
     */
    void markPendingInvoice(Order order, Transaction transaction);

    /**
     * Gọi bởi scheduler (độc lập, không lồng trong transaction nào khác) để gửi request
     * tạo hóa đơn thật sự tới SePay cho các Invoice PENDING chưa có tracking_code.
     */
    void submitPendingInvoice(Invoice invoice);

    /**
     * Gọi bởi scheduler để hỏi SePay kết quả xuất hóa đơn bất đồng bộ và hoàn tất
     * (cập nhật invoice_number/invoice_url + gửi email) khi đã có kết quả.
     */
    void pollPendingInvoice(Invoice invoice);
}
