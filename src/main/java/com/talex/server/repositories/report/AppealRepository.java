package com.talex.server.repositories.report;

import com.talex.server.entities.report.Appeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppealRepository
        extends JpaRepository<Appeal, String>, JpaSpecificationExecutor<Appeal> {

    // Tìm đơn khiếu nại theo ID của hình phạt
    Optional<Appeal> findByPenalty_PenaltyId(String penaltyId);

    // Kiểm tra xem hình phạt này đã được tạo đơn khiếu nại trước đó chưa (mỗi Penalty chỉ khiếu nại 1 lần)
    boolean existsByPenalty_PenaltyId(String penaltyId);
}