package com.talex.server.configs.properties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientProperties {
    private String baseUrl;
    private String apiKey;
    private int connectionTimeoutMs = 5000;
    private int readTimeoutMs = 10000;
}
