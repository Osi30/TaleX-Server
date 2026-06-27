package com.talex.server.configs.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.time.Duration;

@Configuration
@EnableKafkaStreams
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
            log.error("[QUESTDB_FALLBACK_DISK_LOG] - Message thất bại: {}", failedMessage);

        }, backOff);
    }

    @Bean
    public KStream<String, String> kafkaStreamProvider(StreamsBuilder streamsBuilder) {
        // 1. Định nghĩa Serdes (Serializer/Deserializer) để ép kiểu dữ liệu trong Stream
        Serde<String> stringSerde = Serdes.String();

        // 2. Đọc luồng dữ liệu thô từ Topic (Aiven)
        KStream<String, String> rawStream = streamsBuilder.stream("watch-raw", Consumed.with(stringSerde, stringSerde));

        // 3. Xử lý Stream với Session Window
        rawStream
                // Gom nhóm theo Key (sessionId)
                .groupByKey(Grouped.with(stringSerde, stringSerde))

                // Định nghĩa Session Window: Nếu sau 30 giây không nhận thêm Heartbeat mới -> Đóng Session
                .windowedBy(SessionWindows.ofInactivityGapWithNoGrace(Duration.ofSeconds(30)))

                // Thuật toán cộng dồn thời gian (Aggregate)
                .aggregate(
                        () -> "", // Giá trị khởi tạo
                        (sessionId, currentMessage, currentAggValue) -> {
                            String[] parts = currentMessage.split(",");
                            String accountId = parts[1];
                            String episodeId = parts[2];
                            double heartbeatValue = Double.parseDouble(parts[4]);
                            long requestTimestamp = Long.parseLong(parts[5]);

                            long startTime = requestTimestamp;
                            double totalDuration = heartbeatInterval;
                            int heartbeatCount = 1;

                            if (!currentAggValue.isEmpty()) {
                                // Nếu đã có dữ liệu cũ trong cửa sổ 30s này
                                String[] aggParts = currentAggValue.split(",");
                                totalDuration = Double.parseDouble(aggParts[3]);
                                heartbeatCount = Integer.parseInt(aggParts[4]);
                                startTime = Long.parseLong(aggParts[5]);
                                long lastClientTimestamp = Long.parseLong(aggParts[6]);

                                // Kiểm tra gian lận
                                long deltaTimeMs = requestTimestamp - lastClientTimestamp;
                                double deltaTimeSec = deltaTimeMs / 1000.0;

                                // Nếu khoảng cách gửi giữa 2 request quá ngắn so với heartbeatValue
                                if (deltaTimeSec >= (heartbeatValue - 1.0)) {
                                    totalDuration += heartbeatValue;
                                    heartbeatCount += 1;
                                } else {
                                    // Ở bước này có thể xử lí gửi kafka message
                                    // để ghi log vi phạm và lưu vào chỗ xử lí riêng
                                    log.warn("Phát hiện gian lận");
                                }
                            }

                            return sessionId + "," + accountId + "," + episodeId + "," + totalDuration + "," + heartbeatCount + "," + startTime + "," + requestTimestamp;
                        },
                        (sessionId, agg1, agg2) -> agg1.isEmpty() ? agg2 : agg1,
                        Materialized.with(stringSerde, stringSerde)
                )

                // Chuyển kết quả từ dạng KTable (bảng trạng thái) quay lại thành KStream để bắn đi
                .toStream()

                // Định dạng lại Message trước khi bắn sang Topic kết quả
                // Định dạng mới: Key = sessionId, Value = "episodeId,totalDuration"
                .map((windowedKey, finalValue) -> new KeyValue<>(windowedKey.key(), finalValue))

                // Bắn kết quả chốt sổ cuối cùng sang topic tổng hợp trên Aiven
                .to("watch-summary", Produced.with(stringSerde, stringSerde));

        return rawStream;
    }

    /// Batch Factory
    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> batchFactory() {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
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
        return factory;
    }
}
