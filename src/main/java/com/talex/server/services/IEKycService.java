package com.talex.server.services;

import com.talex.server.dtos.responses.idrecognition.IdCardOcrResult;
import com.talex.server.dtos.responses.idrecognition.back.FptAiIdBackResponse;
import com.talex.server.dtos.responses.idrecognition.front.FptAiIdFrontResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

public interface IEKycService {
    IdCardOcrResult processIdCardOcr(MultipartFile frontImage, MultipartFile backImage);

    CompletableFuture<FptAiIdFrontResponse> scanFrontSideAsync(MultipartFile file);

    CompletableFuture<FptAiIdBackResponse> scanBackSideAsync(MultipartFile file);
//    CompletableFuture<FptAiLivenessV3Response> livenessDetectionAsync(MultipartFile video, MultipartFile faceImage);
}
