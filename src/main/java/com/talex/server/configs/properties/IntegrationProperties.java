package com.talex.server.configs.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "integration")
@Data
public class IntegrationProperties {
    private Map<String, ClientProperties> clients = new HashMap<>();
}
