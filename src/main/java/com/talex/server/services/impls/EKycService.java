package com.talex.server.services.impls;

import com.talex.server.dtos.responses.idrecognition.back.FptAiIdBackResponse;
import com.talex.server.dtos.responses.idrecognition.front.FptAiIdFrontResponse;
import com.talex.server.exceptions.details.FptAIIDRecognitionException;
import com.talex.server.services.IEKycService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class EKycService implements IEKycService {
    @Qualifier("fptAiRestClient")
    private final RestClient restClient;

    public EKycService(@Qualifier("fptAiRestClient") RestClient client) {
        this.restClient = client;
    }

    @CircuitBreaker(name = "fptAiFrontOcr", fallbackMethod = "fallbackFrontOcr")
    @Override
    public FptAiIdFrontResponse processFrontSide(MultipartFile file) {
        return sendOcrRequest("vision/idr/vnm", file, FptAiIdFrontResponse.class);
    }

    @CircuitBreaker(name = "fptAiBackOcr", fallbackMethod = "fallbackBackOcr")
    @Override
    public FptAiIdBackResponse processBackSide(MultipartFile file) {
        return sendOcrRequest("vision/idr/vnm", file, FptAiIdBackResponse.class);
    }

    public FptAiIdFrontResponse fallbackFrontOcr(MultipartFile file, Throwable throwable) {
        throw new FptAIIDRecognitionException("Hệ thống nhận diện mặt trước CCCD đang gặp sự cố: " + throwable.getMessage());
    }

    public FptAiIdBackResponse fallbackBackOcr(MultipartFile file, Throwable throwable) {
        throw new FptAIIDRecognitionException("Hệ thống nhận diện mặt sau CCCD đang gặp sự cố: " + throwable.getMessage());
    }

    private <T> T sendOcrRequest(String uri, MultipartFile file, Class<T> responseType) {
        try {
            MultiValueMap<String, Object> body = getStringObjectMultiValueMap(file);

            return restClient.post()
                    .uri(uri)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(responseType);

        } catch (IOException e) {
            throw new FptAIIDRecognitionException("Lỗi cấu trúc luồng đọc dữ liệu ảnh: " + e.getMessage());
        }
    }

    @NotNull
    private static MultiValueMap<String, Object> getStringObjectMultiValueMap(MultipartFile file) throws IOException {
        InputStreamResource inputStreamResource = new InputStreamResource(file.getInputStream()) {
            @Override
            public long contentLength() {
                return file.getSize();
            }

            @Override
            public String getFilename() {
                return file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", inputStreamResource);
        return body;
    }
}
