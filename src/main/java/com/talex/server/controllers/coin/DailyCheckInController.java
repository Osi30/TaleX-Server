package com.talex.server.controllers.coin;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.responses.coin.DailyCheckInResponseDto;
import com.talex.server.dtos.responses.coin.DailyCheckInStatusDto;
import com.talex.server.services.coin.IDailyCheckInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller quản lý Điểm danh hằng ngày.
 *
 * <p>Base path: {@code /api/v1/check-in}</p>
 *
 * <ul>
 *   <li>{@code GET  /status} — Trạng thái điểm danh hôm nay (gọi khi mở app)</li>
 *   <li>{@code POST /}       — Thực hiện điểm danh (idempotent: chỉ thành công 1 lần/ngày)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/check-in")
@RequiredArgsConstructor
@Tag(name = "User - Daily Check-in", description = "API điểm danh hằng ngày và nhận thưởng")
public class DailyCheckInController {

    private final IDailyCheckInService checkInService;

    /**
     * Lấy trạng thái điểm danh của user hôm nay.
     * Frontend gọi khi mở app để hiển thị badge / nút điểm danh.
     *
     * @param accountId ID tài khoản, lấy tự động từ JWT token qua {@code @CurrentAccountId}
     * @return {@link DailyCheckInStatusDto} — đã/chưa điểm danh hôm nay, streak hiện tại
     */
    @GetMapping("/status")
    @Operation(
            summary = "Xem trạng thái điểm danh hôm nay",
            description = "Kiểm tra người dùng đã điểm danh trong ngày hay chưa và trả về chuỗi điểm danh hiện tại."
    )
    public ResponseEntity<BaseResponse> getCheckInStatus(
            @Parameter(hidden = true) @CurrentAccountId UUID accountId
    ) {
        DailyCheckInStatusDto status = checkInService.getCheckInStatus(accountId);

        return ResponseEntity.ok(
                BaseResponse.builder()
                        .code(200)
                        .message("Lấy trạng thái điểm danh thành công")
                        .data(status)
                        .build()
        );
    }

    /**
     * Thực hiện điểm danh hôm nay.
     * <p>
     * Idempotent ở tầng application (Redis Lock) và tầng database (Unique Constraint).
     * Gọi 2 lần trong ngày sẽ nhận lỗi {@code 409 ALREADY_CHECKED_IN}.
     * </p>
     *
     * @param accountId ID tài khoản, lấy tự động từ JWT token qua {@code @CurrentAccountId}
     * @return {@link DailyCheckInResponseDto} — số coin được thưởng và streak mới
     */
    @PostMapping
    @Operation(
            summary = "Thực hiện điểm danh hằng ngày",
            description = "Ghi nhận điểm danh của người dùng, tính chuỗi ngày liên tiếp và cộng phần thưởng Coin tương ứng."
    )
    public ResponseEntity<BaseResponse> performCheckIn(
            @Parameter(hidden = true) @CurrentAccountId UUID accountId
    ) {
        DailyCheckInResponseDto result = checkInService.checkIn(accountId);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                BaseResponse.builder()
                        .code(201)
                        .message("Điểm danh thành công")
                        .data(result)
                        .build()
        );
    }
}
