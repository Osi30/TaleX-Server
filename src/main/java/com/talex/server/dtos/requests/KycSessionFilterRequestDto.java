package com.talex.server.dtos.requests;

import com.talex.server.dtos.BaseFilterRequestDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class KycSessionFilterRequestDto extends BaseFilterRequestDto {
    private String[] statuses;
}
