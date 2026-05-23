package com.talex.server.controllers;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.responses.idrecognition.IdCardOcrResult;
import com.talex.server.services.IEKycService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
public class EKycController {
    private final IEKycService ekycService;

    @PostMapping(value = "/id-card", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse> scanIdCard(
            @RequestParam("frontImage") MultipartFile frontImage,
            @RequestParam("backImage") MultipartFile backImage)
    {
        IdCardOcrResult result = ekycService.processIdCardOcr(frontImage, backImage);

        return ResponseEntity.ok(
                BaseResponse.builder()
                        .code(200)
                        .message("Nhận diện trích xuất CCCD thành công!")
                        .data(result)
                        .build()
        );
    }
}
