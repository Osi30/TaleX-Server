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
}
