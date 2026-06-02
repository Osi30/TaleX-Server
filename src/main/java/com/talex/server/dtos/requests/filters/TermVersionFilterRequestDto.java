package com.talex.server.dtos.requests.filters;

import com.talex.server.dtos.BaseFilterRequestDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class TermVersionFilterRequestDto extends BaseFilterRequestDto {
    private String[] types;
}
