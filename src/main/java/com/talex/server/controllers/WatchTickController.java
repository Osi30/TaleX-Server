package com.talex.server.controllers;

import io.questdb.client.Sender;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/episodes")
@RequiredArgsConstructor
public class WatchTickController {
    private final Sender questDBSender;

    @PostMapping("/tick")
    public ResponseEntity<String> sendWatchTick(
            @RequestParam String userId,
            @RequestParam String contentId,
            @RequestParam int dwellTime
    ) {

        try {
            // 2. LUỒNG LƯU TRỮ LÂU DÀI (QUESTDB): Đẩy thẳng qua cổng mạng ILP
            questDBSender.table("watch_ticks")
                    .symbol("user_id", userId)
                    .symbol("content_id", contentId)
                    .longColumn("dwell_time", dwellTime)
//                    .intColumn("dwell_time", dwellTime)
                    .atNow(); // Tự động lấy timestamp hiện tại của hệ thống làm mốc thời gian log

            questDBSender.flush();

            return ResponseEntity.ok("Tick ghi nhận thành công.");

        } catch (Exception e) {
            // Log lỗi nếu VPS nghẽn hoặc mất kết nối mạng
            return ResponseEntity.status(500).body("Lỗi ghi log hệ thống: " + e.getMessage());
        }
    }
}