package com.talex.server.dtos.responses.invoice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SePayInvoiceDetailDto {
    @JsonProperty("reference_code")
    private String referenceCode;

    @JsonProperty("invoice_number")
    private String invoiceNumber;

    @JsonProperty("issued_date")
    private String issuedDate;

    @JsonProperty("pdf_url")
    private String pdfUrl;

    @JsonProperty("xml_url")
    private String xmlUrl;

    private String status;
}
