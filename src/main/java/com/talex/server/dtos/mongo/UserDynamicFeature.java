package com.talex.server.dtos.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDynamicFeature {

    @Builder.Default
    private List<String> categories = new ArrayList<>();

    @Builder.Default
    private List<String> tags = new ArrayList<>();
}