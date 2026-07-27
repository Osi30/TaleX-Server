package com.talex.server.dtos.responses.invoice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SePayProviderAccountSummaryDto {
    private String id;
    private String provider;
    private boolean active;

    @JsonProperty("tax_authority_approved_date")
    private String taxAuthorityApprovedDate;
}
