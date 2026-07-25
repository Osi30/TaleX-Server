package com.talex.server.controllers.recommend;

import com.talex.server.repositories.trending.AccountImpressionRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/demo/impressions")
@RequiredArgsConstructor
public class AccountImpressionDemoController {

    private final AccountImpressionRepository accountImpressionRepository;

    /**
     * 1. Test Insert Batch lượt hiển thị cho Account và danh sách Series
     * POST /api/v1/demo/impressions/batch
     */
    @PostMapping("/batch")
    @Transactional
    public ResponseEntity<Map<String, Object>> testBatchInsert(@RequestBody BatchInsertReq request) {
        int insertedRows = accountImpressionRepository.insertBatchIfNotExists(
                UUID.fromString(request.getAccountId()),
                String.join(",", request.getSeriesIds())
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("insertedRows", insertedRows);
        response.put("message", "Đã chèn thành công " + insertedRows + " bản ghi mới (bỏ qua các bản ghi trùng).");

        return ResponseEntity.ok(response);
    }

    /**
     * 2. Test Update isInteracted = true
     * PATCH /api/v1/demo/impressions/interact?accountId=...&seriesId=...
     */
    @PatchMapping("/interact")
    @Transactional
    public ResponseEntity<Map<String, Object>> testUpdateInteract(
            @RequestParam String accountId,
            @RequestParam String seriesId) {

        int updatedRows = accountImpressionRepository.updateIsInteractedTrue(UUID.fromString(accountId), seriesId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("updatedRows", updatedRows);
        response.put("message", updatedRows > 0
                ? "Cập nhật isInteracted thành TRUE thành công! Trigger DB đã tăng interactionClick +1."
                : "Không có dòng nào được cập nhật (bản ghi không tồn tại hoặc đã isInteracted = TRUE từ trước).");

        return ResponseEntity.ok(response);
    }

    /**
     * 3. Test Update isWatched = true
     * PATCH /api/v1/demo/impressions/watch?accountId=...&seriesId=...
     */
    @PatchMapping("/watch")
    @Transactional
    public ResponseEntity<Map<String, Object>> testUpdateWatch(
            @RequestParam String accountId,
            @RequestParam String seriesId) {

        int updatedRows = accountImpressionRepository.updateIsWatchedTrue(UUID.fromString(accountId), seriesId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("updatedRows", updatedRows);
        response.put("message", updatedRows > 0
                ? "Cập nhật isWatched thành TRUE thành công! Trigger DB đã tăng engageClick +1."
                : "Không có dòng nào được cập nhật (bản ghi không tồn tại hoặc đã isWatched = TRUE từ trước).");

        return ResponseEntity.ok(response);
    }

    @Data
    public static class BatchInsertReq {
        private String accountId;
        private List<String> seriesIds;
    }
}