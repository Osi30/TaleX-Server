package com.talex.server.controllers.coin;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.coin.CoinEconomyConfigRequestDto;
import com.talex.server.dtos.responses.coin.CoinEconomyConfigResponseDto;
import com.talex.server.services.coin.ICoinEconomyConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller quản lý cấu hình kinh tế Coin dành riêng cho Admin.
 *
 * <p>Base path: {@code /api/v1/admin/coin/economy/config}</p>
 *
 * <ul>
 *   <li>{@code GET /}  — Xem cấu hình phần thưởng hiện hành (từ Cache hoặc DB).</li>
 *   <li>{@code PUT /}  — Cập nhật cấu hình phần thưởng (INSERT bản ghi mới + Evict Cache).</li>
 * </ul>
 *
 * <p>
 * Toàn bộ class được bảo vệ bởi {@code @PreAuthorize("hasRole('ADMIN')")} ở class-level.
 * Mọi request đến bất kỳ endpoint nào đều bắt buộc phải có role ADMIN.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/admin/coin/economy/config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCoinEconomyController {

    private final ICoinEconomyConfigService coinEconomyConfigService;

    /**
     * Lấy cấu hình kinh tế Coin hiện hành.
     * <p>
     * Kết quả được phục vụ từ Caffeine Cache (15 phút) nếu đã có, không query DB.
     * Admin gọi API này khi muốn xem cấu hình đang được áp dụng trước khi quyết định chỉnh sửa.
     * </p>
     *
     * @return {@link CoinEconomyConfigResponseDto} — cấu hình hiện hành kèm thời điểm và người tạo
     */
    @GetMapping
    public ResponseEntity<BaseResponse> getConfig() {
        CoinEconomyConfigResponseDto config = coinEconomyConfigService.getConfig();

        return ResponseEntity.ok(
                BaseResponse.builder()
                        .code(200)
                        .message("Lấy cấu hình kinh tế Coin thành công")
                        .data(config)
                        .build()
        );
    }

    /**
     * Cập nhật cấu hình kinh tế Coin.
     * <p>
     * Mỗi lần gọi API này sẽ INSERT một bản ghi cấu hình mới (không ghi đè bản cũ),
     * sau đó Evict toàn bộ Cache. Lần gọi {@code getConfig()} tiếp theo sẽ query DB
     * và load bản ghi mới nhất vào Cache.
     * </p>
     *
     * @param request  Dữ liệu cấu hình mới — đã được validate bởi {@code @Valid}
     * @param adminId  UUID của Admin đang đăng nhập, lấy tự động từ JWT qua {@code @CurrentAccountId}
     * @return {@link CoinEconomyConfigResponseDto} — bản ghi cấu hình vừa được tạo
     */
    @PutMapping
    public ResponseEntity<BaseResponse> updateConfig(
            @Valid @RequestBody CoinEconomyConfigRequestDto request,
            @CurrentAccountId UUID adminId
    ) {
        CoinEconomyConfigResponseDto updated = coinEconomyConfigService.updateConfig(request, adminId);

        return ResponseEntity.ok(
                BaseResponse.builder()
                        .code(200)
                        .message("Cập nhật cấu hình kinh tế Coin thành công")
                        .data(updated)
                        .build()
        );
    }
}
