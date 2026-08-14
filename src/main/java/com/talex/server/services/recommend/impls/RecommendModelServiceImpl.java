package com.talex.server.services.recommend.impls;

import com.talex.server.dtos.recommend.response.TrainInitResponseDto;
import com.talex.server.services.recommend.RecommendModelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class RecommendModelServiceImpl implements RecommendModelService {

    @Value("${ai.train.token}")
    private String token;

    private final String pythonApi;
    private final RestTemplate restTemplate;

    private static final String TRAIN_INIT_URL = "/api/v1/recommendations/train-init";
    private static final String TRAIN_INIT_REAL_URL = "/api/v1/recommendations/train-init-real";
    private static final String DOWNLOAD_TRAIN_DATA_URL = "/api/v1/recommendations/train-data/download";

    public RecommendModelServiceImpl(@Value("${python.api}") String pythonApi) {
        this.pythonApi = pythonApi;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public TrainInitResponseDto triggerTrainInit() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(pythonApi + TRAIN_INIT_URL);
        if (token != null && !token.isBlank()) {
            builder.queryParam("token", token);
        }

        log.info("[AI Training] Khởi tạo huấn luyện mô hình với Mock Data...");
        ResponseEntity<TrainInitResponseDto> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                HttpEntity.EMPTY,
                TrainInitResponseDto.class
        );

        return response.getBody();
    }

    @Override
    public TrainInitResponseDto triggerTrainInitReal(Integer maxSamples) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(pythonApi + TRAIN_INIT_REAL_URL);
        if (token != null && !token.isBlank()) {
            builder.queryParam("token", token);
        }
        if (maxSamples != null) {
            builder.queryParam("max_samples", maxSamples);
        }

        log.info("[AI Training] Khởi tạo huấn luyện mô hình với DỮ LIỆU THỰC (Postgres + Mongo)...");
        ResponseEntity<TrainInitResponseDto> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                HttpEntity.EMPTY,
                TrainInitResponseDto.class
        );

        return response.getBody();
    }

    @Override
    public ResponseEntity<Resource> downloadTrainData() {
        String url = pythonApi + DOWNLOAD_TRAIN_DATA_URL;
        log.info("[AI Download] Tải tập dữ liệu huấn luyện train_data.xlsx từ Python Server...");

        ResponseEntity<Resource> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                Resource.class
        );

        // Bảo toàn Content-Type và Content-Disposition (filename) từ Python FastAPI gửi về
        HttpHeaders headers = new HttpHeaders();
        if (response.getHeaders().getContentType() != null) {
            headers.setContentType(response.getHeaders().getContentType());
        } else {
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }

        if (response.getHeaders().getContentDisposition() != null) {
            headers.setContentDisposition(response.getHeaders().getContentDisposition());
        } else {
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"train_data.xlsx\"");
        }

        return new ResponseEntity<>(response.getBody(), headers, HttpStatus.OK);
    }
}