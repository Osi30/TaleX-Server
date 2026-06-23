package com.talex.server.controllers.coin.admin;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.coin.MissionRequestDto;
import com.talex.server.entities.coin.Mission;
import com.talex.server.services.coin.IMissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/missions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Missions", description = "API quản trị danh sách nhiệm vụ hệ thống")
public class AdminMissionController {

    private final IMissionService missionService;

    @GetMapping
    @Operation(
            summary = "Lấy toàn bộ cấu hình nhiệm vụ",
            description = "Hiển thị tất cả nhiệm vụ (kể cả đang tắt) cho Admin quản lý."
    )
    public ResponseEntity<BaseResponse> getAllMissions() {
        List<Mission> missions = missionService.getAllMissionsForAdmin();

        return ResponseEntity.ok(
                BaseResponse.builder()
                        .code(200)
                        .message("Lấy danh sách nhiệm vụ quản trị thành công")
                        .data(missions)
                        .build()
        );
    }

    @PostMapping
    @Operation(summary = "Tạo nhiệm vụ mới")
    public ResponseEntity<BaseResponse> createMission(
            @Valid @RequestBody MissionRequestDto request,
            @Parameter(hidden = true) @CurrentAccountId UUID adminId
    ) {
        Mission created = missionService.createMission(request, adminId.toString());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                BaseResponse.builder()
                        .code(201)
                        .message("Tạo nhiệm vụ thành công")
                        .data(created)
                        .build()
        );
    }

    @PutMapping("/{missionId}")
    @Operation(
            summary = "Cập nhật thông tin nhiệm vụ",
            description = "Sửa đổi nội dung, phần thưởng hoặc chỉ tiêu của nhiệm vụ."
    )
    public ResponseEntity<BaseResponse> updateMission(
            @Parameter(description = "ID của nhiệm vụ cần cập nhật", required = true)
            @PathVariable UUID missionId,
            @Valid @RequestBody MissionRequestDto request,
            @Parameter(hidden = true) @CurrentAccountId UUID adminId
    ) {
        Mission updated = missionService.updateMission(
                missionId,
                request,
                adminId.toString()
        );

        return ResponseEntity.ok(
                BaseResponse.builder()
                        .code(200)
                        .message("Cập nhật nhiệm vụ thành công")
                        .data(updated)
                        .build()
        );
    }

    @PatchMapping("/{missionId}/toggle")
    @Operation(
            summary = "Bật/Tắt nhiệm vụ",
            description = "Chuyển đổi trạng thái isActive của nhiệm vụ."
    )
    public ResponseEntity<BaseResponse> toggleMissionStatus(
            @Parameter(description = "ID của nhiệm vụ cần chuyển đổi trạng thái", required = true)
            @PathVariable UUID missionId,
            @Parameter(hidden = true) @CurrentAccountId UUID adminId
    ) {
        Mission updated = missionService.toggleMissionStatus(
                missionId,
                adminId.toString()
        );

        return ResponseEntity.ok(
                BaseResponse.builder()
                        .code(200)
                        .message("Thay đổi trạng thái nhiệm vụ thành công")
                        .data(updated)
                        .build()
        );
    }
}
