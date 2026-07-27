package com.talex.server.services.ads;

import org.springframework.web.multipart.MultipartFile;

public interface IAdMediaUploadService {
    String uploadAdMedia(MultipartFile file);
    String generatePresignedGetUrl(String mediaUrl);
}
