package com.talex.server.services.ads.impls;

import com.talex.server.configs.properties.MediaProperties;
import com.talex.server.services.ads.AdMediaUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdMediaUploadServiceImpl implements AdMediaUploadService {

    private final S3Client s3Client;
    private final MediaProperties mediaProperties;
    private final S3Presigner s3Presigner;

    @Override
    public String uploadAdMedia(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        try {
            MediaProperties.Aws aws = mediaProperties.getAws();
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // Generate a unique S3 key for ad media
            String key = "ads/" + mediaProperties.getAppEnv() + "/" + UUID.randomUUID() + extension;

            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(aws.getBucketName())
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            // Upload directly to S3
            s3Client.putObject(objectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // Build public URL
            String cloudfrontDomain = aws.getCloudfrontDomain();
            if (cloudfrontDomain != null && !cloudfrontDomain.isBlank()) {
                return "https://" + cloudfrontDomain + "/" + key;
            } else {
                return "https://" + aws.getBucketName() + ".s3." + aws.getRegion() + ".amazonaws.com/" + key;
            }
        } catch (IOException e) {
            log.error("Error uploading ad media", e);
            throw new RuntimeException("Failed to upload ad media to S3", e);
        }
    }

    @Override
    public String generatePresignedGetUrl(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isBlank()) return mediaUrl;
        
        MediaProperties.Aws aws = mediaProperties.getAws();
        String bucketName = aws.getBucketName();
        String key = extractS3Key(mediaUrl, aws);
        
        if (key == null) return mediaUrl; // Can't extract key, fallback to original

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
                
        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofDays(7))
                .getObjectRequest(getObjectRequest)
                .build();
                
        return s3Presigner.presignGetObject(getObjectPresignRequest).url().toString();
    }

    private String extractS3Key(String url, MediaProperties.Aws aws) {
        String cloudfrontDomain = aws.getCloudfrontDomain();
        if (cloudfrontDomain != null && !cloudfrontDomain.isBlank() && url.contains(cloudfrontDomain)) {
            return url.substring(url.indexOf(cloudfrontDomain) + cloudfrontDomain.length() + 1);
        }
        String s3Domain = aws.getBucketName() + ".s3." + aws.getRegion() + ".amazonaws.com";
        if (url.contains(s3Domain)) {
            return url.substring(url.indexOf(s3Domain) + s3Domain.length() + 1);
        }
        return null;
    }
}
