package com.talex.server.policies;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FilePolicy {
    // KYC
    KYC_IMAGE(5 * 1024 * 1024L, "5MB", new String[]{"image/jpeg", "image/png"}, "JPEG, PNG"),
    KYC_VIDEO(20 * 1024 * 1024L, "20MB", new String[]{"video/mp4"}, "MP4"),

    // Content
    CONTENT_IMAGE(10 * 1024 * 1024L, "10MB", new String[]{"image/jpeg", "image/png", "image/webp"}, "JPEG, PNG, WEBP"),
    CONTENT_VIDEO(100 * 1024 * 1024L, "100MB", new String[]{"video/mp4", "video/x-msvideo", "video/quicktime"}, "MP4, AVI, MOV"),

    // Khác (Ví dụ: Tài liệu đính kèm)
    DOCUMENT(15 * 1024 * 1024L, "15MB", new String[]{"application/pdf", "application/msword"}, "PDF, DOC");

    private final long maxSizeBytes;
    private final String maxSizeLabel;
    private final String[] allowedContentTypes;
    private final String allowedExtensionsLabel;
}
