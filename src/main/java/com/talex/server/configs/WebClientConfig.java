package com.talex.server.configs;

import com.talex.server.configs.properties.ClientProperties;
import com.talex.server.configs.properties.IntegrationProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {
    private final IntegrationProperties integrationProperties;

    /**
     * Builder dùng chung chứa các cấu hình nền tảng.
     */
    @Bean
    public WebClient.Builder sharedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean(name = "fptAiWebClient")
    public WebClient fptAiWebClient(WebClient.Builder builder) {
        ClientProperties fptConfig = integrationProperties.getClients().get("fpt-ai");

        if (fptConfig == null) {
            throw new IllegalStateException("Chưa cấu hình thông số 'fpt-ai' trong file application.yml!");
        }

        // ĐIỂM TỐI ƯU: Chỉ áp dụng bộ nhớ đệm 50MB RIÊNG cho duy nhất Client này
        ExchangeStrategies ekycStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
                .build();

        // Cấu hình Timeout sâu xuống tầng Netty HttpClient
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, fptConfig.getConnectionTimeoutMs())
                .responseTimeout(Duration.ofMillis(fptConfig.getReadTimeoutMs()))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(fptConfig.getReadTimeoutMs(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(fptConfig.getReadTimeoutMs(), TimeUnit.MILLISECONDS)));

        // Build ra WebClient hoàn chỉnh, tiêm sẵn Base URL và Header API Key định danh
        return builder.clone() // Dùng .clone() để không làm ảnh hưởng tới các WebClient của service khác
                .baseUrl(fptConfig.getBaseUrl())
                .defaultHeader("api_key", fptConfig.getApiKey()) // Tên header do FPT.AI quy định
                .exchangeStrategies(ekycStrategies)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
