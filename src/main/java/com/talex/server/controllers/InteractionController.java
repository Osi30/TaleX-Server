package com.talex.server.controllers;

import com.talex.server.services.IMessagePublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/interactions")
@RequiredArgsConstructor
public class InteractionController {
    private final StringRedisTemplate redisTemplate;
    private final IMessagePublisherService messagePublisher;

    @PostMapping("/like")
    public ResponseEntity<String> userLikeContent(
            @RequestParam String accountId,
            @RequestParam String episodeId) {

        // 1. TĂNG BỘ ĐẾM REAL-TIME TRÊN REDIS (Cho hệ thống đề xuất bốc nhanh)
        String redisKey = "content:" + episodeId + ":like_count";
        redisTemplate.opsForValue().increment(redisKey);

        // 2. BẮN SỰ KIỆN VÀO KAFKA HÀNG ĐỢI (Để worker lưu ngầm vào PostgreSQL sau)
        // Tạo chuỗi json đơn giản để test
        String eventJson = String.format("{\"userId\":\"%s\", \"contentId\":\"%s\", \"action\":\"LIKE\"}", accountId, episodeId);
        messagePublisher.publishInteractionEvent(eventJson);

        // ĐÁP ỨNG SIÊU NHANH VỀ CHO USER
        return ResponseEntity.ok("Like thành công! Đang xử lý ngầm...");
    }
}
