package com.talex.server.configs;

import com.talex.server.configs.properties.ClientProperties;
import com.talex.server.configs.properties.IntegrationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {
    private final IntegrationProperties integrationProperties;

    @Bean(name = "fptAiRestClient")
    public RestClient fptAiRestClient() {
        // Đọc lại cấu hình y hệt bên WebClientConfig
        ClientProperties fptConfig = integrationProperties.getClients().get("fpt-ai");

        if (fptConfig == null) {
            throw new IllegalStateException("Chưa cấu hình thông số 'fpt-ai' trong file application.yml!");
        }

        // Cấu hình Timeout sâu xuống tầng HTTP Request Factory hạ tầng
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) fptConfig.getConnectionTimeoutMs()); // Cấu hình Connect Timeout
        requestFactory.setReadTimeout((int) fptConfig.getReadTimeoutMs());       // Cấu hình Read Timeout

        // Khởi tạo RestClient hoàn chỉnh, tiêm sẵn Base URL và Header API Key định danh
        return RestClient.builder()
                .baseUrl(fptConfig.getBaseUrl())
                .defaultHeader("api_key", fptConfig.getApiKey()) // Tên header do FPT.AI quy định
                .requestFactory(requestFactory)
                .build();
    }
}
