package com.talex.server.enums;

public enum NotificationType {
    // Reports
    REPORT_RESULT, PENALTY_WARNING, APPEAL_RESULT, SYSTEM_NOTICE,
    // Content pipeline — Admin ép ẩn/gỡ ép ẩn episode sau khi đã duyệt
    EPISODE_FORCE_HIDDEN, EPISODE_RESTORED
}
