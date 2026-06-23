package com.talex.server.repositories.coin;

import com.talex.server.entities.coin.UserMissionProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserMissionProgressRepository extends JpaRepository<UserMissionProgress, UUID> {

    /**
     * Lấy toàn bộ tiến độ nhiệm vụ của một user trong một ngày cụ thể.
     * Dùng khi frontend cần hiển thị danh sách nhiệm vụ kèm tiến độ hôm nay.
     *
     * @param accountId ID tài khoản
     * @param date      Ngày cần truy vấn (thường là LocalDate.now())
     * @return Danh sách tiến độ của user trong ngày đó
     */
    List<UserMissionProgress> findByAccountIdAndProgressDate(UUID accountId, LocalDate date);

    /**
     * Tìm tiến độ của một user đối với một nhiệm vụ cụ thể trong một ngày.
     * Dùng trong luồng cập nhật tiến độ để kiểm tra đã tạo bản ghi chưa (upsert pattern).
     *
     * @param accountId ID tài khoản
     * @param missionId ID nhiệm vụ
     * @param date      Ngày cần truy vấn
     * @return Optional tiến độ nếu tìm thấy
     */
    Optional<UserMissionProgress> findByAccountIdAndMissionIdAndProgressDate(
            UUID accountId, UUID missionId, LocalDate date);
}
