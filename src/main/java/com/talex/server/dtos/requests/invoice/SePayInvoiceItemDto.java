package com.talex.server.dtos.requests.invoice;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SePayInvoiceItemDto {
    @JsonProperty("line_number")
    private Integer lineNumber;

    @JsonProperty("line_type")
    private Integer lineType;

    @JsonProperty("item_code")
    private String itemCode;

    @JsonProperty("item_name")
    private String itemName;

    private String unit;

    private BigDecimal quantity;

    @JsonProperty("unit_price")
    private Long unitPrice;
}
