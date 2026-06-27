package com.talex.server.dtos.requests.filters;

import com.talex.server.dtos.BaseFilterRequestDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class CreatorTierFilterRequestDto extends BaseFilterRequestDto {
}
