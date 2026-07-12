package com.talex.server.configs.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class KafkaConfig {
    @Value("${heartbeat.interval}")
    private Double heartbeatInterval;

    private final ConsumerFactory<Object, Object> consumerFactory;

    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        // Thử lại 3 lần, mỗi lần cách nhau 2 giây
        FixedBackOff backOff = new FixedBackOff(2000L, 3);

        return new DefaultErrorHandler((consumerRecord, exception) -> {
            // Khi đã thử lại 3 lần thất bại (QuestDB vẫn sập), nhảy vào đây:
            String failedMessage = (String) consumerRecord.value();

            // In ra một Logger riêng biệt (Cấu hình Logback đẩy riêng dòng này ra file questdb-fallback.log)
            log.error("[FALLBACK_DISK_LOG] - Message thất bại: {}", failedMessage);

        }, backOff);
    }

    /// Batch Factory
    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> batchFactory() {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.setCommonErrorHandler(kafkaErrorHandler());
        factory.setConcurrency(2);
        return factory;
    }

    /// Single Factory
    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> singleFactory() {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(false);
        factory.setCommonErrorHandler(kafkaErrorHandler());
        return factory;
    }
}
