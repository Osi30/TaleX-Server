package com.talex.server.services.admin;

import com.talex.server.dtos.responses.admin.AdminWatermarkResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface AdminWatermarkService {
    AdminWatermarkResponseDto extractWatermark(MultipartFile file, String mediaType);
}
