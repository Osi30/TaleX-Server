package com.talex.server.records;

public record CloudinaryUploadResult(
        String secureUrl,
        String publicId,
        String resourceType,
        String format,
        Long bytes,
        Integer width,
        Integer height,
        Long duration
) {
}
