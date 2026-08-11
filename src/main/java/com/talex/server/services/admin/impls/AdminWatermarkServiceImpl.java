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

    @Value("${python.api:http://localhost:8000}/watermark/extract")
    private String aiExtractUrl;

    @Override
    public AdminWatermarkResponseDto extractWatermark(MultipartFile file, String mediaType) {
        try {
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            String lineEnd = "\r\n";
            String twoHyphens = "--";

            java.net.URL url = new java.net.URL(aiExtractUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (java.io.DataOutputStream dos = new java.io.DataOutputStream(connection.getOutputStream())) {
                // 1. media_type parameter
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"media_type\"" + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes(mediaType + lineEnd);

                // 2. file parameter
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" + lineEnd);
                dos.writeBytes("Content-Type: application/octet-stream" + lineEnd);
                dos.writeBytes(lineEnd);

                // Write file data
                try (java.io.InputStream fileInputStream = file.getInputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        dos.write(buffer, 0, bytesRead);
                    }
                }
                dos.writeBytes(lineEnd);

                // 3. End boundary
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                dos.flush();
            }

            int serverResponseCode = connection.getResponseCode();
            if (serverResponseCode >= 200 && serverResponseCode < 300) {
                try (java.io.InputStream is = connection.getInputStream()) {
                    JsonNode root = objectMapper.readTree(is);
                    String creatorId = root.has("creator_id") ? root.get("creator_id").asText() : null;
                    String viewerId = root.has("viewer_id") ? root.get("viewer_id").asText() : null;
                    String message = root.has("message") ? root.get("message").asText() : null;

                    return AdminWatermarkResponseDto.builder()
                            .creatorId(creatorId)
                            .viewerId(viewerId)
                            .message(message)
                            .build();
                }
            } else {
                try (java.io.InputStream es = connection.getErrorStream()) {
                    String errorMsg = es != null ? new String(es.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8) : "Unknown error";
                    log.error("AI Watermark API returned error: {} - {}", serverResponseCode, errorMsg);
                    
                    String displayMessage = "Lỗi từ AI Server khi trích xuất Watermark.";
                    try {
                        JsonNode errorNode = objectMapper.readTree(errorMsg);
                        if (errorNode.has("detail")) {
                            displayMessage = errorNode.get("detail").asText();
                        } else if (errorNode.has("message")) {
                            displayMessage = errorNode.get("message").asText();
                        }
                    } catch (Exception parseEx) {
                        // Bỏ qua lỗi parse, dùng message mặc định
                    }
                    throw new IllegalArgumentException(displayMessage);
                }
            }
        } catch (Exception e) {
            log.error("Error calling AI Watermark API: ", e);
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            if (e instanceof RuntimeException && !e.getMessage().startsWith("Gặp lỗi")) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Gặp lỗi khi liên kết tới máy chủ AI để quét bản quyền: " + e.getMessage());
        }
    }
}