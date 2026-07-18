package com.talex.server.dtos.responses.invoice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * All SePay eInvoice API responses wrap the payload in this {success, data} envelope.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SePayEnvelopeDto<T> {
    private boolean success;
    private T data;
}
