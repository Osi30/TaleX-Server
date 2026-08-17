package com.talex.server.controllers.admin;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.media.MediaPurgeRequestDto;
import com.talex.server.services.media.MediaPurgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin-only irreversible media purge (hard-delete). Distinct from the Creator soft-delete endpoint
 * {@code DELETE /api/v1/media/{id}} — this permanently erases the media from Postgres, deletes its
 * Milvus fingerprint, and removes the real S3/CloudFront assets. ADMIN role ONLY (never STAFF): the
 * action cannot be undone. A mandatory reason is recorded to a legal audit log.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Admin Media Purge", description = "Xóa vĩnh viễn nội dung (hard-delete) — chỉ Admin, không thể hoàn tác")
public class AdminMediaPurgeController {

    private final MediaPurgeService mediaPurgeService;

    @DeleteMapping("/api/v1/admin/media/{mediaId}/purge")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa vĩnh viễn 1 Media (Postgres + Milvus + S3). Chỉ Admin, không thể hoàn tác.")
    public ResponseEntity<BaseResponse> purge(
            @PathVariable String mediaId,
            @Valid @RequestBody MediaPurgeRequestDto request,
            @CurrentAccountId UUID adminId) {
        mediaPurgeService.purge(mediaId, adminId.toString(), request.getReason());
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Media permanently purged")
                .data(null)
                .build());
    }
}
