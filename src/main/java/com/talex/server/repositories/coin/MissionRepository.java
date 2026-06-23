package com.talex.server.repositories.coin;

import com.talex.server.entities.coin.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MissionRepository extends JpaRepository<Mission, UUID> {

    /**
     * Lấy danh sách tất cả nhiệm vụ đang được bật (isActive = true).
     * Dùng để hiển thị danh sách nhiệm vụ cho User phía client.
     *
     * @return Danh sách Mission đang hoạt động
     */
    List<Mission> findByIsActiveTrue();

    /**
     * Tìm nhiệm vụ theo mã code (business key).
     * Dùng khi service nhận event từ hệ thống khác và cần map sang Mission tương ứng.
     *
     * @param code Mã nhiệm vụ (VD: "WATCH_AD_DAILY", "ONLINE_30_MIN")
     * @return Optional Mission nếu tìm thấy
     */
    Optional<Mission> findByCode(String code);
}
