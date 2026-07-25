package com.talex.server.entities.interaction;

import com.talex.server.entities.auth.Account;
import com.talex.server.entities.series.Series;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "account_impressions",
        indexes = {
                @Index(name = "idx_account_impression_acc_series", columnList = "account_id, series_id"),
                @Index(name = "idx_account_impression_series", columnList = "series_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountImpression {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "series_id", nullable = false)
    private Series series;

    // Đánh dấu người dùng có tương tác với Series (like, bookmark, comment, share...)
    @Column(name = "is_interacted", nullable = false)
    @Builder.Default
    private Boolean isInteracted = false;

    // Đánh dấu người dùng đã click VÀ ở lại đọc >= 5 giây
    @Column(name = "is_watched", nullable = false)
    @Builder.Default
    private Boolean isWatched = false;
}
