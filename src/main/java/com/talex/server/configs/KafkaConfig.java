package com.talex.server.configs;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.time.Duration;

@Configuration
@EnableKafkaStreams
@Slf4j
public class KafkaConfig {
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
                // Giả sử message thô gửi lên Kafka dạng String phân tách dấu phẩy: "sessionId,episodeId,duration"
                // Ta chuyển Key của Message từ rỗng thành "sessionId" để gom nhóm
//                .selectKey((k, v) -> v.split(",")[0])

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
                            long currentDuration = Long.parseLong(parts[3]);
                            long requestTimestamp = Long.parseLong(parts[4]);
                            long startTime = requestTimestamp;

                            // Session đã tồn tại
                            if (!currentAggValue.isEmpty()) {
                                String[] aggParts = currentAggValue.split(",");
                                startTime = Long.parseLong(aggParts[4]);
                            } else {
                                System.out.println("Empty");
                            }

                            return sessionId + "," + accountId + "," + episodeId + "," + currentDuration + "," + startTime + "," + requestTimestamp;
                        },
                        (sessionId, aggValue1, aggValue2) -> {
                            // Merger xử lý khi Kafka Streams vô tình gộp 2 Session Window lại làm một
                            if (aggValue1.isEmpty()) return aggValue2;
                            if (aggValue2.isEmpty()) return aggValue1;

                            String[] p1 = aggValue1.split(",");
                            String[] p2 = aggValue2.split(",");

                            String accountId = p1[0];
                            String episodeId = p1[1];

                            // Vì FE tự cộng dồn, khi gộp 2 session, lấy duration lớn nhất
                            long duration = Math.min(Long.parseLong(p1[3]), Long.parseLong(p2[3]));

                            // Lấy thời gian bắt đầu sớm nhất
                            long startTime = Math.min(Long.parseLong(p1[4]), Long.parseLong(p2[4]));

                            // Lấy thời gian kết thúc muộn nhất
                            long endTime = Math.max(Long.parseLong(p1[5]), Long.parseLong(p2[5]));

                            return sessionId + "," + accountId + "," + episodeId + "," + duration + "," + startTime + "," + endTime;
                        }, // Hàm merge khi 2 session bị gộp
                        Materialized.with(stringSerde, stringSerde)
                )

                // Chuyển kết quả từ dạng KTable (bảng trạng thái) quay lại thành KStream để bắn đi
                .toStream()

                // Định dạng lại Message trước khi bắn sang Topic kết quả
                // Định dạng mới: Key = sessionId, Value = "episodeId,totalDuration"
                .map((windowedKey, finalValue) -> {
                    String sessionId = windowedKey.key(); // Lấy lại sessionId nguyên bản từ Window
                    return new KeyValue<>(sessionId, finalValue);
                })

                // Bắn kết quả chốt sổ cuối cùng sang topic tổng hợp trên Aiven
                .to("watch-summary", Produced.with(stringSerde, stringSerde));

        return rawStream;
    }
}
