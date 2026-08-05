package com.talex.server.entities.subscription;

import com.talex.server.entities.auth.Account;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "account_subscription")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "account_subscription_id")
    private String accountSubscriptionId;

    // Order gốc khi subscription được cấp qua thanh toán — null nếu Admin/Staff cấp thủ
    // công (xem AccountSubscriptionRequestDto). Dùng để tra Invoice hiển thị ở lịch sử mua
    // Premium — trước đây field này bị SubscriptionOrderFulfillmentService truyền vào rồi
    // âm thầm rơi mất vì entity không có cột lưu, khiến lịch sử Premium không bao giờ hiện
    // hóa đơn dù Invoice vẫn được tạo bình thường trong DB.
    @Column(name = "order_id")
    private String orderId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "total_views")
    @Builder.Default
    private Long totalViews = 0L;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "is_cancelled", nullable = false)
    @Builder.Default
    private Boolean isCancelled = false;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @OneToMany(mappedBy = "accountSubscription", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SubscriptionStat> subscriptionStats = new ArrayList<>();
}
