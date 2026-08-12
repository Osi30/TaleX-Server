package com.talex.server.controllers.series;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.services.media.ManifestGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Playback Manifest", description = "API trộn HLS Manifest (A/B Watermarking) động cho từng người dùng")
public class PlaybackManifestController {
    
    private final ManifestGeneratorService manifestGeneratorService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/api/v1/episodes/{id}/playback/manifest.m3u8", produces = "application/vnd.apple.mpegurl")
    @Operation(summary = "Lấy Dynamic HLS Manifest (A/B Watermarking)")
    public ResponseEntity<String> getDynamicManifest(
            @PathVariable("id") String episodeId,
            @CurrentAccountId UUID accountId) {
        
        String manifestContent = manifestGeneratorService.generateDynamicManifest(episodeId, accountId.toString());
        return ResponseEntity.ok(manifestContent);
    }
}
