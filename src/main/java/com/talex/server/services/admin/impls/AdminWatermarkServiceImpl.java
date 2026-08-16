package com.talex.server.services.admin.impls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.responses.admin.AdminWatermarkResponseDto;
import com.talex.server.dtos.responses.auth.AdminAccountResponseDto;
import com.talex.server.entities.auth.Account;
import com.talex.server.repositories.auth.AccountRepository;
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
    private final AccountRepository accountRepository;

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
                    log.info("AI Server Raw Response: {}", root.toString());
                    String creatorId = root.hasNonNull("creator_id") ? root.get("creator_id").asText() : null;
                    String viewerId = root.hasNonNull("viewer_id") ? root.get("viewer_id").asText() : null;
                    
                    if (creatorId == null) {
                        String creatorIdAudio = root.hasNonNull("creator_id_audio") ? root.get("creator_id_audio").asText() : null;
                        String creatorIdFingerprint = root.hasNonNull("creator_id_fingerprint") ? root.get("creator_id_fingerprint").asText() : null;
                        creatorId = determineBestCreatorId(creatorIdAudio, creatorIdFingerprint);
                    }

                    // Map ViewerID Binary to real UUID
                    if (viewerId != null && viewerId.startsWith("User_Binary_")) {
                        String binaryStr = viewerId.replace("User_Binary_", "");
                        String realViewerId = findAccountIdByBinaryPattern(binaryStr);
                        if (realViewerId != null) {
                            viewerId = realViewerId;
                        }
                    }

                    String message = root.hasNonNull("message") ? root.get("message").asText() : null;

                    AdminAccountResponseDto creatorAccount = null;
                    if (creatorId != null) {
                        try {
                            Account account = accountRepository.findById(java.util.UUID.fromString(creatorId)).orElse(null);
                            if (account != null) {
                                creatorAccount = toAdminAccountDto(account);
                            }
                        } catch (Exception e) {
                            log.warn("Invalid Creator ID format: {}", creatorId);
                        }
                    }

                    AdminAccountResponseDto viewerAccount = null;
                    if (viewerId != null) {
                        try {
                            Account account = accountRepository.findById(java.util.UUID.fromString(viewerId)).orElse(null);
                            if (account != null) {
                                viewerAccount = toAdminAccountDto(account);
                            }
                        } catch (Exception e) {
                            log.warn("Invalid Viewer ID format: {}", viewerId);
                        }
                    }

                    return AdminWatermarkResponseDto.builder()
                            .creatorId(creatorId)
                            .viewerId(viewerId)
                            .message(message)
                            .creatorAccount(creatorAccount)
                            .viewerAccount(viewerAccount)
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
                log.error("Extract Watermark error", e);
            }
            throw new RuntimeException("Lỗi trích xuất Watermark: " + e.getMessage());
        }
    }

    private AdminAccountResponseDto toAdminAccountDto(Account account) {
        return AdminAccountResponseDto.builder()
                .accountId(account.getAccountId())
                .email(account.getEmail())
                .username(account.getUsername())
                .fullName(account.getFullName())
                .avatarUrl(account.getAvatarUrl())
                .roleName(account.getRole() == null ? null : account.getRole().getCode())
                .status(account.getStatus() == null ? null : account.getStatus().name())
                .createdAt(account.getCreatedAt())
                .build();
    }

    private String determineBestCreatorId(String audioId, String fingerprintId) {
        log.info("Xác định Creator ID. Audio: {}, Fingerprint: {}", audioId, fingerprintId);
        
        String bestAudioMatch = null;
        if (audioId != null && !audioId.isBlank()) {
            bestAudioMatch = findClosestAccountId(audioId);
        }

        String bestFpMatch = null;
        if (fingerprintId != null && !fingerprintId.isBlank()) {
            if (isValidAndExists(fingerprintId)) {
                bestFpMatch = fingerprintId;
            }
        }

        if (bestAudioMatch != null && bestFpMatch != null) {
            if (bestAudioMatch.equals(bestFpMatch)) {
                log.info("Audio và Fingerprint đều match trùng 1 User: {}", bestAudioMatch);
                return bestAudioMatch;
            } else {
                log.info("Audio và Fingerprint ra 2 User khác nhau. Ưu tiên Audio: {}", bestAudioMatch);
                return bestAudioMatch; // Audio is a hard watermark, usually more reliable if matched
            }
        }

        if (bestAudioMatch != null) {
            log.info("Chỉ tìm thấy Audio ID: {}", bestAudioMatch);
            return bestAudioMatch;
        }

        if (bestFpMatch != null) {
            log.info("Chỉ tìm thấy Fingerprint ID: {}", bestFpMatch);
            return bestFpMatch;
        }

        return null;
    }

    private boolean isValidAndExists(String id) {
        try {
            java.util.UUID uuid = java.util.UUID.fromString(id);
            return accountRepository.existsById(uuid);
        } catch (Exception e) {
            return false;
        }
    }

    private String findClosestAccountId(String extractedString) {
        if (extractedString == null || extractedString.length() < 10) return null;

        java.util.List<java.util.UUID> allIds = accountRepository.findAllAccountIds();
        
        String bestMatchId = null;
        int maxMatchedLength = 0;

        for (java.util.UUID id : allIds) {
            String target = id.toString();
            int matchScore = calculateMaxConsecutiveMatch(target, extractedString);
            
            if (matchScore > maxMatchedLength) {
                maxMatchedLength = matchScore;
                bestMatchId = target;
            }
        }
        
        // Cần khớp ít nhất 10 ký tự liên tiếp để tránh nhận diện sai
        if (maxMatchedLength >= 10) {
            log.info("TÌM THẤY Creator UUID gần giống nhất: {} (Khớp liên tiếp {}/36 char)", bestMatchId, maxMatchedLength);
            return bestMatchId;
        }
        
        return null;
    }

    private String findAccountIdByBinaryPattern(String extractedBinary) {
        log.info("Bắt đầu truy ngược UUID từ chuỗi nhị phân AI: {}", extractedBinary);
        if (extractedBinary == null || extractedBinary.length() < 5) {
            log.warn("Chuỗi nhị phân quá ngắn (<5 bit), từ chối đối chiếu.");
            return null;
        }
        
        java.util.List<java.util.UUID> allIds = accountRepository.findAllAccountIds();
        log.info("Đang kiểm tra chéo với {} tài khoản trong hệ thống...", allIds.size());
        
        String bestMatchId = null;
        int maxMatchedLength = 0;

        for (java.util.UUID id : allIds) {
            String hashBinary = convertToBinaryPattern(id.toString());
            
            // Tìm chuỗi con dài nhất khớp nhau (Longest Common Substring)
            // hoặc kiểm tra độ tương đồng. Vì video có thể bị nhiễu 1-2 bit, ta cho phép sai số nhỏ.
            // Cách đơn giản nhất: Kiểm tra xem có chuỗi con nào >= 80% độ dài của extractedBinary nằm trong hash không.
            int matchScore = calculateMaxConsecutiveMatch(hashBinary, extractedBinary);
            
            if (matchScore > maxMatchedLength) {
                maxMatchedLength = matchScore;
                bestMatchId = id.toString();
            }
        }
        
        // Nếu độ dài khớp liên tiếp >= 5 bit, ta coi như tìm thấy
        if (maxMatchedLength >= 5) {
            log.info("TÌM THẤY! UUID: {} (Độ dài khớp: {}/{} bit)", bestMatchId, maxMatchedLength, extractedBinary.length());
            return bestMatchId;
        }
        
        log.warn("Không tìm thấy UUID nào khớp đủ điều kiện (Max khớp: {} bit)", maxMatchedLength);
        return null;
    }

    private int calculateMaxConsecutiveMatch(String source, String target) {
        // Tìm chuỗi con chung dài nhất (Longest Common Substring)
        int max = 0;
        for (int i = 0; i < source.length(); i++) {
            for (int j = 0; j < target.length(); j++) {
                int len = 0;
                while (i + len < source.length() && j + len < target.length() 
                       && source.charAt(i + len) == target.charAt(j + len)) {
                    len++;
                }
                if (len > max) {
                    max = len;
                }
            }
        }
        return max;
    }

    private String convertToBinaryPattern(String viewerId) {
        if (viewerId == null || viewerId.isBlank()) {
            return "0";
        }
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(viewerId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder binary = new StringBuilder();
            for (byte b : hash) {
                for (int i = 7; i >= 0; i--) {
                    binary.append((b >> i) & 1);
                }
            }
            return binary.toString();
        } catch (Exception e) {
            log.error("MD5 error", e);
            return "0";
        }
    }
}