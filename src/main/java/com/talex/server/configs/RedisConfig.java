package com.talex.server.configs;

import com.talex.server.workers.InteractionWorker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.Executor;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }

    /**
     * CẤU HÌNH BỘ PHANH (THREAD POOL EXECUTOR)
     * Tiết lập giới hạn công suất xử lý để bảo vệ RAM của Spring Boot.
     */
    @Bean(name = "redisStreamExecutor")
    public Executor redisStreamExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        // KHÓA CỨNG tối đa là 4 thread, không cho phình RAM ra thêm
        executor.setMaxPoolSize(4);
        // Bộ đệm hàng đợi tạm thời trong bộ nhớ RAM Spring
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("RedisStream-Worker-");
        executor.initialize();

        return executor;
    }

    /**
     * CẤU HÌNH ÔNG LẮNG NGHE REDIS STREAM (STREAM CONTAINER)
     * Đóng vai trò như một bộ điều tốc. Nếu Thread Pool ở trên bị đầy (Postgres chậm),
     * Container này sẽ chủ động CHẶN (Block) không pull thêm message từ Redis về nữa.
     */
    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            InteractionWorker streamConsumer,
            Executor redisStreamExecutor
    ) {
        // Thiết lập tùy chọn cho Container đọc Stream
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .batchSize(100) // Mỗi lần kéo tối đa 100 bản ghi từ Redis về xử lý
                        .executor(redisStreamExecutor)
                        .pollTimeout(Duration.ofSeconds(3)) // Thời gian đợi nếu Stream trống rỗng
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);

        // Đăng ký lắng nghe kênh Stream "interaction:stream" với nhóm tiêu thụ "pg-sync-group"
        container.receive(
                Consumer.from("pg-sync-group", "worker-1"),
                StreamOffset.create(
                        "interaction:stream",
                        ReadOffset.lastConsumed()
                ),
                streamConsumer
        );

        container.start();
        return container;
    }
}
