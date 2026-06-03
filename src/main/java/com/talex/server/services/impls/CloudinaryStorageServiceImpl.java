package com.talex.server.services.impls;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.talex.server.configs.properties.CloudinaryProperties;
import com.talex.server.enums.MediaType;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.records.CloudinaryUploadResult;
import com.talex.server.services.CloudinaryStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryStorageServiceImpl implements CloudinaryStorageService {
    private final Cloudinary cloudinary;
    private final CloudinaryProperties properties;

    @Override
    public CloudinaryUploadResult upload(MultipartFile file, MediaType mediaType) {
        ensureConfigured();

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("talex-upload-", extensionOf(file.getOriginalFilename()));
            file.transferTo(tempFile);

            Map<String, Object> options = ObjectUtils.asMap(
                    "resource_type", resourceType(mediaType),
                    "folder", properties.getFolder(),
                    "use_filename", true,
                    "unique_filename", true,
                    "overwrite", false
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> result = mediaType == MediaType.VIDEO
                    ? cloudinary.uploader().uploadLarge(tempFile.toFile(), options)
                    : cloudinary.uploader().upload(tempFile.toFile(), options);

            return new CloudinaryUploadResult(
                    stringValue(result.get("secure_url")),
                    stringValue(result.get("public_id")),
                    stringValue(result.get("resource_type")),
                    stringValue(result.get("format")),
                    longValue(result.get("bytes")),
                    intValue(result.get("width")),
                    intValue(result.get("height")),
                    durationValue(result.get("duration"))
            );
        } catch (IOException exception) {
            throw new ContentModuleException(
                    4500,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot upload media to Cloudinary",
                    exception);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // Temporary-file cleanup failure should not change upload result.
                }
            }
        }
    }

    private void ensureConfigured() {
        if (!properties.isConfigured()) {
            throw ContentModuleException.badRequest(
                    "Cloudinary is not configured. Set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET");
        }
    }

    private String resourceType(MediaType mediaType) {
        return mediaType == MediaType.VIDEO ? "video" : "image";
    }

    private String extensionOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".bin";
        }
        return fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private Long durationValue(Object value) {
        if (value instanceof Number number) {
            return Math.round(number.doubleValue());
        }
        return null;
    }
}
