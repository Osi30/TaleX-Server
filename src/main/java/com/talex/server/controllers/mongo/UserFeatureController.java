package com.talex.server.controllers.mongo;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.mongo.UserDynamicFeature;
import com.talex.server.dtos.mongo.UserFeatureRequest;
import com.talex.server.entities.mongo.UserFeatureDocument;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.services.mongo.UserFeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mongo/features/user")
@RequiredArgsConstructor
@Tag(name = "User Feature Controller", description = "APIs quản lý đặc điểm và thói quen của người dùng")
public class UserFeatureController {
    private final UserFeatureService featureService;
    private final AccountRepository accountRepository;

    @PreAuthorize("isAuthenticated()")
    @PostMapping()
    @Operation(
            summary = "Lưu hoặc cập nhật tính năng của người dùng",
            description = "Lưu thông tin đặc điểm người dùng mới hoặc cập nhật đặc điểm hiện tại của người dùng"
    )
    public ResponseEntity<UserFeatureDocument> saveUserFeatures(
            @CurrentAccountId UUID accountId,
            @Valid @RequestBody UserFeatureRequest request,
            HttpServletRequest servletRequest
    ) {
        request.setLanguage(servletRequest.getLocale().toLanguageTag());
        UserFeatureDocument savedData = featureService
                .saveOrUpdateFeatures(accountId.toString(), request);
        return ResponseEntity.ok(savedData);
    }

    @GetMapping()
    @Operation(
            summary = "Lấy thông tin đặc điểm của người dùng",
            description = "Truy vấn và trả về dữ liệu đặc điểm của người dùng"
    )
    public ResponseEntity<UserFeatureDocument> getUserFeatures(
            @CurrentAccountId UUID accountId
    ) {
        return featureService.getFeaturesByUserId(accountId.toString())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Lấy thông tin đặc điểm của người dùng bằng id",
            description = "Truy vấn và trả về dữ liệu đặc điểm của người dùng"
    )
    public ResponseEntity<UserFeatureDocument> getUserFeaturesById(
            @PathVariable String id
    ) {
        return featureService.getFeaturesByUserId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/static")
    @Operation(
            summary = "Lấy thông tin đặc điểm của người dùng",
            description = "Truy vấn và trả về dữ liệu đặc điểm của người dùng"
    )
    public ResponseEntity<?> getUserStaticFeatures(
            @CurrentAccountId UUID accountId
    ) {
        return ResponseEntity.ok(featureService
                .getUserStaticFeatureByAccountId(accountId.toString()));
    }

    @GetMapping("/dynamic")
    @Operation(
            summary = "Lấy thông tin đặc điểm động của người dùng",
            description = "Truy vấn và trả về danh sách Top 5 tên danh mục và thẻ được người dùng xem nhiều nhất"
    )
    public ResponseEntity<UserDynamicFeature> getUserDynamicFeatures(
            @CurrentAccountId UUID accountId
    ) {
        return ResponseEntity.ok(featureService
                .getUserDynamicFeatureByAccountId(accountId.toString()));
    }

    @GetMapping("/{id}/dynamic")
    @Operation(
            summary = "Lấy thông tin đặc điểm động của người dùng bằng id",
            description = "Truy vấn và trả về danh sách Top 5 tên danh mục và thẻ được người dùng xem nhiều nhất"
    )
    public ResponseEntity<UserDynamicFeature> getUserDynamicFeaturesById(
            @PathVariable String id
    ) {
        return ResponseEntity.ok(featureService
                .getUserDynamicFeatureByAccountId(id));
    }

    @PostMapping("/stats/reset")
    @Operation(
            summary = "Kích hoạt clean up",
            description = "Kích hoạt clean up dữ liệu 24h và 7d của nhiều accounts"
    )
    public ResponseEntity<String> cleanSync(
            @RequestParam() String[] accountIds,
            @RequestParam Boolean is24h,
            @RequestParam Boolean is7d
    ) {
        List<String> ids = List.of(accountIds);
        if (is24h) {
            featureService.cleanUp24hFeatures(ids);
            accountRepository.updateIs24hByAccountIds(ids.stream().map(UUID::fromString).toList());
        }

        if (is7d) {
            featureService.cleanUp7dFeatures(ids);
            accountRepository.updateIs7dByAccountIds(ids.stream().map(UUID::fromString).toList());
        }
        return ResponseEntity.ok("Đã kích hoạt và hoàn tất dọn dẹp toàn bộ Series Stats thành công!");
    }
}