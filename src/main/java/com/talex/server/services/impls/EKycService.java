package com.talex.server.services.impls;

import com.talex.server.dtos.responses.idrecognition.IdCardOcrResult;
import com.talex.server.dtos.responses.idrecognition.back.BackData;
import com.talex.server.dtos.responses.idrecognition.back.FptAiIdBackResponse;
import com.talex.server.dtos.responses.idrecognition.front.FptAiIdFrontResponse;
import com.talex.server.dtos.responses.idrecognition.front.FrontData;
import com.talex.server.exceptions.details.FptAIIDRecognitionException;
import com.talex.server.services.IEKycService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class EKycService implements IEKycService {
    private final WebClient webClient;

    /// Gọi API ID Recognition để lấy dữ liệu trên mặt trước CCCD
    @Override
    @Async("ekycExecutor")
    public CompletableFuture<FptAiIdFrontResponse> scanFrontSideAsync(MultipartFile file) {
        return CompletableFuture.completedFuture(
                executeOcrRequest("vision/idr/vnm", file, FptAiIdFrontResponse.class)
        );
    }

    /// Gọi API ID Recognition để lấy dữ liệu trên mặt sau CCCD
    @Override
    @Async("ekycExecutor")
    public CompletableFuture<FptAiIdBackResponse> scanBackSideAsync(MultipartFile file) {
        return CompletableFuture.completedFuture(
                executeOcrRequest("vision/idr/vnm", file, FptAiIdBackResponse.class)
        );
    }

    /// Gọi API ID Recognition để lấy dữ liệu trên 2 mặt ảnh CCCD
    @Override
    public IdCardOcrResult processIdCardOcr(MultipartFile frontImage, MultipartFile backImage) {
        validateIdFiles(frontImage, backImage);

        // Quét 2 mặt song song
        CompletableFuture<FptAiIdFrontResponse> frontFuture = scanFrontSideAsync(frontImage);
        CompletableFuture<FptAiIdBackResponse> backFuture = scanBackSideAsync(backImage);

        // Đợi cả 2 tác vụ mạng hoàn thành cùng lúc
        CompletableFuture.allOf(frontFuture, backFuture).join();

        try {
            FptAiIdFrontResponse frontResponse = frontFuture.get();
            FptAiIdBackResponse backResponse = backFuture.get();

            // Kiểm tra mã lỗi
            validateFptResponse(frontResponse.getErrorCode(), frontResponse.getErrorMessage(), "Mặt trước");
            validateFptResponse(backResponse.getErrorCode(), backResponse.getErrorMessage(), "Mặt sau");

            if (frontResponse.getData() == null || frontResponse.getData().isEmpty() ||
                    backResponse.getData() == null || backResponse.getData().isEmpty()) {
                throw new FptAIIDRecognitionException("FPT.AI không thể trích xuất dữ liệu từ hình ảnh cung cấp.");
            }

            FrontData frontData = frontResponse.getData().getFirst();
            BackData backData = backResponse.getData().getFirst();

            // Mapper
            return IdCardOcrResult.builder()
                    .idNumber(frontData.getId())
                    .fullName(frontData.getName())
                    .dob(frontData.getDob())
                    .sex(frontData.getSex())
                    .nationality(frontData.getNationality())
                    .address(frontData.getAddress())
                    .expiryDate(frontData.getDoe())
                    .issueDate(backData.getIssueDate())
                    .issuePlace(backData.getIssueLoc())
                    .personalFeatures(backData.getFeatures())
                    .build();
        } catch (ExecutionException | InterruptedException e) {
            throw new FptAIIDRecognitionException("Xử lý bất đồng bộ nhận diện CCCD thất bại: " + e.getMessage());
        }
    }

    /// Kiểm tra nếu có mã lỗi trùng khớp
    private void validateFptResponse(int errorCode, String errorMessage, String side) {
        if (errorCode != 0) {
            throw new FptAIIDRecognitionException(String.format("Lỗi FPT.AI Id Recognition (%s): %s (Mã lỗi: %d)", side, errorMessage, errorCode));
        }
    }

    /// Kiểm tra files hợp lệ
    private void validateIdFiles(MultipartFile frontImage, MultipartFile backImage){
        if (frontImage.isEmpty() || backImage.isEmpty()) {
            throw new FptAIIDRecognitionException("ID Front Image is Empty");
        }

        if (backImage.isEmpty()) {
            throw new FptAIIDRecognitionException("ID Back Image is Empty");
        }
    }

    /// Gọi API tới ID Recognition
    private <T> T executeOcrRequest(String uri, MultipartFile file, Class<T> responseType) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("image", new ByteArrayResource(file.getBytes()))
                    .filename(Objects.requireNonNull(file.getOriginalFilename()))
                    .contentType(MediaType.IMAGE_JPEG);

            return webClient.post()
                    .uri(uri)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(responseType)
                    .block(); // Chặn cục bộ trong Thread Pool riêng của @Async, không gây nghẽn Main Thread
        } catch (IOException e) {
            throw new FptAIIDRecognitionException("Lỗi đọc dữ liệu binary của hình ảnh cccd");
        }
    }
}
