package com.talex.server.services;

import com.talex.server.enums.MediaType;
import com.talex.server.records.CloudinaryUploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryStorageService {
    CloudinaryUploadResult upload(MultipartFile file, MediaType mediaType);
}
