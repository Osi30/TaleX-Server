package com.talex.server.dtos.requests.creator;

import com.talex.server.enums.creator.CreatorIdentityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorVerifiedResultDto {
    private CreatorIdentityStatus status;
    private String verifiedNote;
}
