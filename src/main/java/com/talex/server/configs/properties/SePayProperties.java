package com.talex.server.configs.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sepay")
@Data
public class SePayProperties {
    private String accountNumber = "";
    private String bankName = "";
    private String webhookApiKey = "";
    private String qrBaseUrl = "https://qr.sepay.vn/img";
    private Integer orderExpiryMinutes = 30;
    private Integer retryBlockWindowMinutes = 5;

    /**
     * Some banks (e.g. VietinBank) only forward balance-change notifications to
     * SePay when the transfer content starts with a bank-specific keyword
     * (VietinBank requires "SEVQR "). Empty by default; set per linked bank.
     */
    private String transferContentPrefix = "";

    // SePay eInvoice API (https://developer.sepay.vn/vi/einvoice-api/v1/tong-quan)
    private String einvoiceBaseUrl = "https://einvoice-api-sandbox.sepay.vn";
    private String einvoiceUsername = "";
    private String einvoicePassword = "";
}
