package com.talex.server.dtos.requests.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SePayWebhookPayloadDto {
    private Long id;
    private String gateway;

    @JsonProperty("transactionDate")
    private String transactionDate;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("subAccount")
    private String subAccount;

    private String code;
    private String content;

    @JsonProperty("transferType")
    private String transferType;

    private String description;

    @JsonProperty("transferAmount")
    private BigDecimal transferAmount;

    private BigDecimal accumulated;

    @JsonProperty("referenceCode")
    private String referenceCode;
}
