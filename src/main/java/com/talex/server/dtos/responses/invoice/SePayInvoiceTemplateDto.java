package com.talex.server.dtos.responses.invoice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SePayInvoiceTemplateDto {
    @JsonProperty("template_code")
    private String templateCode;

    @JsonProperty("invoice_series")
    private String invoiceSeries;

    @JsonProperty("invoice_label")
    private String invoiceLabel;
}
