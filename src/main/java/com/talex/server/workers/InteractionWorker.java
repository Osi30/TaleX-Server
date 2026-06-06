package com.talex.server.workers;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InteractionWorker {

    @KafkaListener(topics = "user-interactions", groupId = "talex-interaction-group")
    public void consumeLikeEvent(String message) {
        // Worker chạy ngầm bốc dữ liệu ra một cách từ tốn
        System.out.println("Worker nhận được sự kiện từ Kafka để chuẩn bị lưu PostgreSQL: " + message);

        // Khúc này sau này bạn sẽ gọi Repository của PostgreSQL vào để ghi đè hoặc chèn dữ liệu trạng thái:
        // userLikeRepository.save(new UserLike(userId, contentId));
    }
}
