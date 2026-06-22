package com.talex.server.services.coin;

import com.talex.server.dtos.requests.coin.CoinEconomyConfigRequestDto;
import com.talex.server.dtos.responses.coin.CoinEconomyConfigResponseDto;

import java.util.UUID;

/**
 * Contract cho nghiệp vụ quản lý cấu hình kinh tế Coin.
 * <p>
 * Tầng Controller chỉ được inject interface này, KHÔNG inject Repository.
 * </p>
 */
public interface ICoinEconomyConfigService {

    /**
     * Lấy cấu hình kinh tế hiện hành của hệ thống.
     * <p>
     * Kết quả được cache bằng Caffeine (key = "CURRENT_CONFIG").
     * Nếu chưa có bản ghi nào trong DB, tự động khởi tạo và lưu default values.
     * </p>
     *
     * @return Cấu hình mới nhất (hoặc default nếu chưa có)
     */
    CoinEconomyConfigResponseDto getConfig();

    /**
     * Admin cập nhật cấu hình kinh tế Coin.
     * <p>
     * Tạo bản ghi MỚI thay vì ghi đè → giữ nguyên lịch sử mọi lần thay đổi.
     * Evict cache sau khi lưu thành công để hệ thống dùng config mới ngay lập tức.
     * </p>
     *
     * @param request  Dữ liệu cấu hình mới từ Admin
     * @param adminId  UUID của Admin thực hiện thao tác (dùng cho audit trail)
     * @return Bản ghi cấu hình vừa được tạo
     */
    CoinEconomyConfigResponseDto updateConfig(CoinEconomyConfigRequestDto request, UUID adminId);
}
