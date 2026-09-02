package com.talex.server.services.media.impls;

import com.talex.server.entities.media.Media;
import com.talex.server.enums.media.MediaStatus;
import com.talex.server.enums.media.MediaType;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.media.MediaRepository;
import com.talex.server.services.media.ManifestGeneratorService;
import com.talex.server.services.media.MediaProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManifestGeneratorServiceImpl implements ManifestGeneratorService {

    private final MediaRepository mediaRepository;
    private final MediaProviderService mediaProviderService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String generateDynamicManifest(String episodeId, String viewerId) {
        Media media = mediaRepository
                .findFirstByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalseOrderByCreatedAtDesc(
                        episodeId,
                        MediaType.VIDEO,
                        List.of(MediaStatus.ACTIVE, MediaStatus.HLS_READY))
                .orElseThrow(() -> ContentModuleException.notFound("Playable video media not found for episode: " + episodeId));

        String fileUrl = media.getFileUrl();
        // Kiểm tra xem fileUrl có phải là đường dẫn A/B HLS hay không
        if (fileUrl == null || !fileUrl.contains("/ab_hls/")) {
            throw ContentModuleException.badRequest("VIDEO_NOT_SUPPORT_AB_WATERMARK");
        }

        // Thời hạn ký URL: 10 phút (đủ để stream hết 1 episode)
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        // Tải nội dung manifest của version A (làm chuẩn)
        // Phải ký URL trước khi fetch vì CloudFront yêu cầu Signed URL
        String masterAUrl = fileUrl + "/version_A/playlist.m3u8";
        String signedMasterAUrl = mediaProviderService.signSingleUrl(masterAUrl, expiresAt);

        String manifestContent;
        try {
            manifestContent = restTemplate.getForObject(signedMasterAUrl, String.class);
        } catch (Exception e) {
            log.error("Failed to fetch manifest A from S3: {}", masterAUrl, e);
            throw ContentModuleException.badRequest("MANIFEST_NOT_FOUND");
        }

        if (manifestContent == null) {
            throw ContentModuleException.badRequest("MANIFEST_EMPTY");
        }

        // Chuyển đổi ViewerID (ví dụ: UUID) thành chuỗi nhị phân
        String binaryPattern = convertToBinaryPattern(viewerId);

        // Trộn các chunk — mỗi URL chunk phải được ký để client tải được
        StringBuilder dynamicManifest = new StringBuilder();
        String[] lines = manifestContent.split("\n");
        int chunkIndex = 0;

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                continue;
            }
            if (trimmedLine.startsWith("#") || trimmedLine.contains(".key")) {
                dynamicManifest.append(trimmedLine).append("\n");
            } else if (trimmedLine.endsWith(".ts") || trimmedLine.contains(".ts")) {
                // Là dòng chứa link tới chunk
                char bit = binaryPattern.charAt(chunkIndex % binaryPattern.length());
                String chunkName = trimmedLine;

                String chunkUrl;
                if (bit == '1') {
                    chunkUrl = fileUrl + "/version_A/" + chunkName;
                } else {
                    chunkUrl = fileUrl + "/version_B/" + chunkName;
                }

                // Ký URL chunk trước khi ghi vào manifest trả về client
                String signedChunkUrl = mediaProviderService.signSingleUrl(chunkUrl, expiresAt);
                dynamicManifest.append(signedChunkUrl).append("\n");
                chunkIndex++;
            } else {
                dynamicManifest.append(trimmedLine).append("\n");
            }
        }

        return dynamicManifest.toString();
    }

    private String convertToBinaryPattern(String viewerId) {
        if (viewerId == null || viewerId.isBlank()) {
            return "0"; // Mặc định trả về toàn 0 nếu chưa đăng nhập
        }
        try {
            // Lấy hash của ViewerID để đảm bảo pattern phân tán
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(viewerId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder binary = new StringBuilder();
            for (byte b : hash) {
                // Lấy 8 bit của mỗi byte
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
