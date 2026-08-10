package com.talex.server.services.admin.impls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.responses.admin.AdminWatermarkResponseDto;
import com.talex.server.services.admin.AdminWatermarkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminWatermarkServiceImpl implements AdminWatermarkService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.watermark.extract-url:http://localhost:8000/api/v1/watermark/extract}")
    private String aiExtractUrl;

    @Override
    public AdminWatermarkResponseDto extractWatermark(MultipartFile file, String mediaType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // Wrap the file into a ByteArrayResource that preserves the original filename
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
                }
            };
            
            body.add("file", fileResource);
            body.add("media_type", mediaType);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(aiExtractUrl, requestEntity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String creatorId = root.has("creator_id") ? root.get("creator_id").asText() : null;
                String message = root.has("message") ? root.get("message").asText() : null;
                
                return AdminWatermarkResponseDto.builder()
                        .creatorId(creatorId)
                        .message(message)
                        .build();
            } else {
                log.error("AI Watermark API returned error: {}", response.getStatusCode());
                throw new RuntimeException("Lỗi từ AI Server khi trích xuất Watermark.");
            }
        } catch (Exception e) {
            log.error("Error calling AI Watermark API: ", e);
            throw new RuntimeException("Gặp lỗi khi liên kết tới máy chủ AI để quét bản quyền: " + e.getMessage());
        }
    }
}
