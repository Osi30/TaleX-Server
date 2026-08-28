package com.talex.server.enums;

public enum NotificationType {
    // Reports
    REPORT_RESULT, PENALTY_WARNING, APPEAL_RESULT, SYSTEM_NOTICE,
    // Content pipeline — Admin ép ẩn/gỡ ép ẩn episode sau khi đã duyệt
    EPISODE_FORCE_HIDDEN, EPISODE_RESTORED,
    // Content pipeline — Staff duyệt/từ chối nội dung đang chờ kiểm duyệt (18+/bản quyền)
    CONTENT_APPROVED, CONTENT_REJECTED,
    // Payment — thanh toán thành công
    SUBSCRIPTION_PURCHASE_SUCCESS, COMBO_PURCHASE_SUCCESS, EPISODE_PURCHASE_SUCCESS
}
