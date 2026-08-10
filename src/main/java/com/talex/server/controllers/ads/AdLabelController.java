package com.talex.server.controllers.ads;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.ads.AdLabelRequestDto;
import com.talex.server.services.ads.AdLabelService;
import com.talex.server.annotations.CurrentAccountId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ads/labels")
@RequiredArgsConstructor
public class AdLabelController {

    private final AdLabelService adLabelService;

    @GetMapping
    public ResponseEntity<BaseResponse> getAllLabels(
            @CurrentAccountId UUID accountId
    ) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Success")
                .data(adLabelService.getAllLabels(accountId))
                .build());
    }

    @PostMapping
    public ResponseEntity<BaseResponse> createLabel(
            @CurrentAccountId UUID accountId,
            @Valid @RequestBody AdLabelRequestDto requestDto
    ) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Success")
                .data(adLabelService.createLabel(accountId, requestDto))
                .build());
    }

    @PutMapping("/{labelId}")
    public ResponseEntity<BaseResponse> updateLabel(
            @CurrentAccountId UUID accountId,
            @PathVariable UUID labelId,
            @Valid @RequestBody AdLabelRequestDto requestDto
    ) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Success")
                .data(adLabelService.updateLabel(accountId, labelId, requestDto))
                .build());
    }

    @DeleteMapping("/{labelId}")
    public ResponseEntity<BaseResponse> deleteLabel(
            @CurrentAccountId UUID accountId,
            @PathVariable UUID labelId
    ) {
        adLabelService.deleteLabel(accountId, labelId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Success")
                .build());
    }
}
