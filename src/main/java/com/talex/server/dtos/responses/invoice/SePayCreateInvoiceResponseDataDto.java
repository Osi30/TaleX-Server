package com.talex.server.dtos.responses.invoice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SePayCreateInvoiceResponseDataDto {
    @JsonProperty("tracking_code")
    private String trackingCode;

    @JsonProperty("tracking_url")
    private String trackingUrl;

    private String message;
}
