package com.talex.server.controllers.interaction;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.interaction.request.RatingRequest;
import com.talex.server.dtos.interaction.response.AccountRatingResponse;
import com.talex.server.dtos.interaction.response.SeriesRatingResponse;
import com.talex.server.services.interaction.IAccountRatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Account Ratings", description = "API quản lý đánh giá (Rating) Series của người dùng")
public class AccountRatingController {

    private final IAccountRatingService ratingService;

    @Operation(summary = "Đánh giá hoặc Cập nhật điểm đánh giá cho Series")
    @PostMapping("/series/{seriesId}/rate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> upsertRating(
            @CurrentAccountId UUID accountId,
            @Valid @RequestBody RatingRequest requestDto
    ) {
        requestDto.setAccountId(accountId);
        ratingService.upsertRating(requestDto);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Đánh giá series thành công.")
                .build());
    }

    @Operation(summary = "Xóa đánh giá của tài khoản đối với Series")
    @DeleteMapping("/series/{seriesId}/rate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> deleteRating(
            @CurrentAccountId UUID accountId,
            @PathVariable String seriesId
    ) {
        ratingService.deleteRating(accountId, seriesId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Xóa đánh giá thành công.")
                .build());
    }

    @Operation(summary = "Lấy danh sách tất cả các series mà tài khoản hiện tại đã đánh giá")
    @GetMapping("/ratings/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> getMyRatings(
            @CurrentAccountId UUID accountId,
            @ParameterObject @PageableDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Slice<AccountRatingResponse> myRatings = ratingService.getRatingsByAccount(accountId, pageable);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách series đã đánh giá thành công.")
                .data(myRatings)
                .build());
    }

    @Operation(summary = "Lấy danh sách tất cả các lượt đánh giá của một Series cụ thể")
    @GetMapping("/series/{seriesId}/ratings")
    public ResponseEntity<BaseResponse> getSeriesRatings(
            @PathVariable String seriesId,
            @ParameterObject @PageableDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Slice<SeriesRatingResponse> seriesRatings = ratingService.getRatingsBySeries(seriesId, pageable);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách lượt đánh giá của series thành công.")
                .data(seriesRatings)
                .build());
    }
}