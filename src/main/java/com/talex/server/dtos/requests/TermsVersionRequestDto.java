package com.talex.server.dtos.requests;

import com.talex.server.enums.TermsType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TermsVersionRequestDto {
    private String version;
    private String title;
    private TermsType type;
    private String content;
    private Boolean isActive;
}
