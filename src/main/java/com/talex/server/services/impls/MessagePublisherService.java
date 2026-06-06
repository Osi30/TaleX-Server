package com.talex.server.services.impls;

import com.talex.server.services.IMessagePublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessagePublisherService implements IMessagePublisherService {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publishInteractionEvent(String message) {
        // Bắn sự kiện vào topic tên là "user-interactions"
        kafkaTemplate.send("user-interactions", message);
    }
}
