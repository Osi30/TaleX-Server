package com.talex.server.controllers;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.EpisodeRequestDto;
import com.talex.server.dtos.requests.ScheduledPublishRequestDto;
import com.talex.server.services.EpisodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Episode", description = "Các API để quản lý các tập (episode) thuộc về một season, bao gồm quản lý nội dung, cài đặt trả phí (mở khóa) và lên lịch xuất bản.")
public class EpisodeController {
    private final EpisodeService episodeService;

     @PreAuthorize("isAuthenticated()")
    @PostMapping("/api/v1/seasons/{seasonId}/episodes")
    @Operation(summary = "Tạo tập mới", description = "Tạo một tập mới trong một season cụ thể với trạng thái mặc định là DRAFT. Nếu không truyền số tập (episodeNumber), hệ thống tự động tăng. Định dạng nội dung (ContentType) bắt buộc phải khớp với Series. Có thể thiết lập chế độ miễn phí (FREE) hoặc trả phí (PAID). Yêu cầu quyền sở hữu nội dung.")
    public ResponseEntity<BaseResponse> create(
            @PathVariable String seasonId,
            @Valid @RequestBody EpisodeRequestDto request,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(201, "Episode created",
                        episodeService.create(seasonId, request, accountId.toString())));
    }

     @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/v1/seasons/{seasonId}/episodes")
    @Operation(summary = "Lấy danh sách tập theo season", description = "Lấy danh sách tất cả các tập của một season, được sắp xếp tăng dần theo số tập (episodeNumber). Yêu cầu quyền quản lý nội dung (hoặc quyền Admin/Staff).")
    public ResponseEntity<BaseResponse> listBySeason(
            @PathVariable String seasonId,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "OK",
                episodeService.listBySeason(seasonId, accountId.toString())));
    }

     @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/v1/episodes/{id}")
    @Operation(summary = "Lấy chi tiết tập", description = "Lấy toàn bộ thông tin chi tiết của một tập. Yêu cầu quyền sở hữu nội dung.")
    public ResponseEntity<BaseResponse> getById(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "OK", episodeService.getById(id, accountId.toString())));
    }

     @PreAuthorize("isAuthenticated()")
    @PutMapping("/api/v1/episodes/{id}")
    @Operation(summary = "Cập nhật thông tin tập", description = "Cập nhật các trường thông tin (tiêu đề, mô tả, số tập, cài đặt giá tiền). Nếu thay đổi trạng thái sang PUBLISHED, hệ thống sẽ kiểm tra xem tập đã có media (Video/Image) được duyệt (APPROVED) và sẵn sàng hay chưa. Yêu cầu quyền sở hữu nội dung.")
    public ResponseEntity<BaseResponse> update(
            @PathVariable String id,
            @Valid @RequestBody EpisodeRequestDto request,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Episode updated",
                episodeService.update(id, request, accountId.toString())));
    }

     @PreAuthorize("isAuthenticated()")
    @PatchMapping("/api/v1/episodes/{id}/schedule-publish")
    @Operation(summary = "Lên lịch xuất bản tập", description = "Lên lịch để xuất bản tập vào một thời điểm trong tương lai. Yêu cầu tập này phải có ít nhất một media đã sẵn sàng và được duyệt. Nếu đây là tập đầu tiên, hệ thống sẽ tự động lên lịch hiển thị cho cả Season và Series cha. Yêu cầu quyền sở hữu nội dung.")
    public ResponseEntity<BaseResponse> schedulePublish(
            @PathVariable String id,
            @Valid @RequestBody ScheduledPublishRequestDto request,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Episode publish scheduled",
                episodeService.schedulePublish(id, request.getScheduledPublishAt(), accountId.toString())));
    }

    @PatchMapping("/api/v1/episodes/{id}/cancel-schedule")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Hủy lịch xuất bản episode", description = "Hủy lịch xuất bản đã đặt cho episode và chuyển trạng thái về DRAFT. Yêu cầu quyền sở hữu nội dung.")
    public ResponseEntity<BaseResponse> cancelSchedule(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Episode schedule canceled", episodeService.cancelSchedule(id, accountId.toString())));
    }

     @PreAuthorize("isAuthenticated()")
    @PatchMapping("/api/v1/episodes/{id}/publish")
    @Operation(summary = "Xuất bản tập ngay lập tức", description = "Chuyển trạng thái tập sang PUBLISHED. Yêu cầu bắt buộc phải có ít nhất một media (Video hoặc Image) đã xử lý xong và được duyệt (APPROVED). Yêu cầu quyền sở hữu nội dung.")
    public ResponseEntity<BaseResponse> publish(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Episode published", episodeService.publish(id, accountId.toString())));
    }

     @PreAuthorize("isAuthenticated()")
    @PatchMapping("/api/v1/episodes/{id}/hide")
    @Operation(summary = "Ẩn tập", description = "Chuyển trạng thái của tập sang HIDDEN để tạm thời gỡ khỏi chế độ hiển thị công khai. Yêu cầu quyền sở hữu nội dung.")
    public ResponseEntity<BaseResponse> hide(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Episode hidden", episodeService.hide(id, accountId.toString())));
    }

     @PreAuthorize("isAuthenticated()")
    @PatchMapping("/api/v1/episodes/{id}/unhide")
    @Operation(summary = "Bỏ ẩn tập", description = "Khôi phục trạng thái từ HIDDEN về lại PUBLISHED. Hệ thống sẽ kiểm tra lại tính hợp lệ của media đính kèm. Yêu cầu quyền sở hữu nội dung.")
    public ResponseEntity<BaseResponse> unhide(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Episode visible", episodeService.unhide(id, accountId.toString())));
    }

     @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/api/v1/episodes/{id}")
    @Operation(summary = "Xóa tập", description = "Xóa mềm (soft-delete) tập bằng cách chuyển trạng thái sang DELETED. Yêu cầu quyền sở hữu nội dung.")
    public ResponseEntity<BaseResponse> delete(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        episodeService.delete(id, accountId.toString());
        return ResponseEntity.ok(response(200, "Episode deleted", null));
    }

    private BaseResponse response(int code, String message, Object data) {
        return BaseResponse.builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }
}