package com.talex.server.dtos.responses.invoice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SePayProviderAccountDetailDto {
    private String id;
    private String provider;
    private boolean active;

    @JsonProperty("tax_authority_approved_date")
    private String taxAuthorityApprovedDate;

    private List<SePayInvoiceTemplateDto> templates;
}
