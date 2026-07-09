package com.talex.server.entities.series;

import com.talex.server.entities.AnalyticData;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "series_log", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"series_id", "hour_bucket"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeriesLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "series_log_id")
    private String seriesLogId;

    @Column(name = "hour_bucket", nullable = false)
    private LocalDateTime hourBucket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = false)
    private Series series;

    @Embedded
    @Builder.Default
    private AnalyticData analyticData = new AnalyticData();
}
