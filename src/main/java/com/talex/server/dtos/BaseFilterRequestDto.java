package com.talex.server.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BaseFilterRequestDto {
    private Map<String, Object> criteria;
    private String sortBy;
    private String sortDirection;
    private Integer page;
    private Integer pageSize;
}
