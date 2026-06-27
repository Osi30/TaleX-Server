package com.talex.server.dtos.requests.creator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorRegisterDto {
    private String termsId;

    @JsonIgnore
    private UUID accountId;
}
