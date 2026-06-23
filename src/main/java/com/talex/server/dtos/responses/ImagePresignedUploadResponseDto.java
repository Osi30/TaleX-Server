package com.talex.server.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagePresignedUploadResponseDto {

    /** Presigned S3 PUT URL — expires in 1 hour */
    private String uploadUrl;

    /** S3 object key, e.g. images/prod/covers/a1b2c3d4.jpg */
    private String key;

    /** Permanent public URL via CloudFront (or S3 fallback) */
    private String publicUrl;

    private String bucket;

    private String region;
}
