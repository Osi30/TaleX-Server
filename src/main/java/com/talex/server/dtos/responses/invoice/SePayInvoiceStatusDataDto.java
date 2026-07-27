package com.talex.server.dtos.responses.invoice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SePayInvoiceStatusDataDto {
    @JsonProperty("reference_code")
    private String referenceCode;

    // "Success" | "Failed"
    private String status;

    private String message;

    private SePayInvoiceDetailDto invoice;
}
