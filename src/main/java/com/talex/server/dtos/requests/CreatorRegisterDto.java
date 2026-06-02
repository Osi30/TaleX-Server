package com.talex.server.dtos.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorRegisterDto {
    private String termsId;
    private String accountId;
    private Boolean isAcceptTermAlready;
}
