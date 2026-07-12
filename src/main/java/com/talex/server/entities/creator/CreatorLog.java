package com.talex.server.entities.creator;

import com.talex.server.entities.Account;
import com.talex.server.entities.AnalyticData;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "creator_log", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"account_id", "hour_bucket"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorLog implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "creator_log_id")
    private String creatorLogId;

    @Column(name = "hour_bucket", nullable = false)
    private LocalDateTime hourBucket;

    // Đổi quan hệ từ Creator sang Account
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Embedded
    @Builder.Default
    private AnalyticData analyticData = new AnalyticData();

    @Column(name = "follows", nullable = false, columnDefinition = "bigint default 0")
    @Builder.Default
    private Long follows = 0L;
}