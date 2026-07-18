package com.talex.server.dtos.requests.invoice;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SePayCreateInvoiceRequestDto {
    @JsonProperty("template_code")
    private String templateCode;

    @JsonProperty("invoice_series")
    private String invoiceSeries;

    @JsonProperty("issued_date")
    private String issuedDate;

    private String currency;

    @JsonProperty("provider_account_id")
    private String providerAccountId;

    @JsonProperty("reference_code")
    private String referenceCode;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("is_draft")
    private Boolean isDraft;

    private SePayInvoiceBuyerDto buyer;

    private List<SePayInvoiceItemDto> items;

    private String notes;

    @JsonProperty("total_amount")
    private Long totalAmount;
}
