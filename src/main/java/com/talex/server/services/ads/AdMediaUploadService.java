package com.talex.server.services.ads;

import org.springframework.web.multipart.MultipartFile;

public interface AdMediaUploadService {
    String uploadAdMedia(MultipartFile file);
    String generatePresignedGetUrl(String mediaUrl);
}
