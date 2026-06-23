package com.talex.server.services.coin;

import com.talex.server.dtos.responses.coin.DailyCheckInResponseDto;
import com.talex.server.dtos.responses.coin.DailyCheckInStatusDto;

import java.util.UUID;

/**
 * Contract cho nghiệp vụ Điểm danh hằng ngày.
 */
public interface IDailyCheckInService {

    /**
     * Thực hiện điểm danh cho user.
     * Luồng: Acquire Redis Lock → gọi {@link #executeCheckInTransaction} qua proxy → Release Lock.
     *
     * @param accountId ID tài khoản thực hiện điểm danh
     * @return Thông tin phần thưởng và streak sau khi điểm danh thành công
     */
    DailyCheckInResponseDto checkIn(UUID accountId);

    /**
     * Lấy trạng thái điểm danh hiện tại của user.
     * Dùng để frontend vẽ UI khi user mở ứng dụng (không cần lock).
     *
     * @param accountId ID tài khoản cần kiểm tra
     * @return Trạng thái đã/chưa điểm danh hôm nay và streak hiện tại
     */
    DailyCheckInStatusDto getCheckInStatus(UUID accountId);

    /**
     * Thực thi logic điểm danh trong DB Transaction.
     * <p>
     * Phải khai báo ở interface để Spring AOP Proxy có thể intercept
     * khi {@link #checkIn} gọi qua self-injection ({@code self.executeCheckInTransaction()}).
     * </p>
     *
     * @param accountId ID tài khoản thực hiện điểm danh
     * @return Kết quả điểm danh
     */
    DailyCheckInResponseDto executeCheckInTransaction(UUID accountId);
}
