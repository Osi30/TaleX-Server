package com.talex.server.services.recommend.impls;

import com.talex.server.dtos.recommend.response.TrainInitResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendModelServiceImpl Tests")
class RecommendModelServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    private RecommendModelServiceImpl recommendModelService;
    private final String pythonApi = "http://localhost:8000";

    @BeforeEach
    void setUp() {
        recommendModelService = new RecommendModelServiceImpl(pythonApi);
        ReflectionTestUtils.setField(recommendModelService, "restTemplate", restTemplate);
    }

    @Test
    @DisplayName("triggerTrainInit - With valid token")
    void triggerTrainInit_WithToken() {
        ReflectionTestUtils.setField(recommendModelService, "token", "secret-token");
        TrainInitResponseDto expectedDto = new TrainInitResponseDto();
        ResponseEntity<TrainInitResponseDto> responseEntity = new ResponseEntity<>(expectedDto, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(pythonApi + "/api/v1/recommendations/train-init?token=secret-token"),
                eq(HttpMethod.POST),
                eq(HttpEntity.EMPTY),
                eq(TrainInitResponseDto.class)
        )).thenReturn(responseEntity);

        TrainInitResponseDto result = recommendModelService.triggerTrainInit();

        assertThat(result).isSameAs(expectedDto);
    }

    @Test
    @DisplayName("triggerTrainInit - Without token (null or blank)")
    void triggerTrainInit_WithoutToken() {
        ReflectionTestUtils.setField(recommendModelService, "token", "");
        TrainInitResponseDto expectedDto = new TrainInitResponseDto();
        ResponseEntity<TrainInitResponseDto> responseEntity = new ResponseEntity<>(expectedDto, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(pythonApi + "/api/v1/recommendations/train-init"),
                eq(HttpMethod.POST),
                eq(HttpEntity.EMPTY),
                eq(TrainInitResponseDto.class)
        )).thenReturn(responseEntity);

        TrainInitResponseDto result = recommendModelService.triggerTrainInit();

        assertThat(result).isSameAs(expectedDto);
    }

    @Test
    @DisplayName("triggerTrainInitReal - With token and maxSamples")
    void triggerTrainInitReal_WithTokenAndMaxSamples() {
        ReflectionTestUtils.setField(recommendModelService, "token", "secret-token");
        TrainInitResponseDto expectedDto = new TrainInitResponseDto();
        ResponseEntity<TrainInitResponseDto> responseEntity = new ResponseEntity<>(expectedDto, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(pythonApi + "/api/v1/recommendations/train-init-real?token=secret-token&max_samples=100"),
                eq(HttpMethod.POST),
                eq(HttpEntity.EMPTY),
                eq(TrainInitResponseDto.class)
        )).thenReturn(responseEntity);

        TrainInitResponseDto result = recommendModelService.triggerTrainInitReal(100);

        assertThat(result).isSameAs(expectedDto);
    }

    @Test
    @DisplayName("triggerTrainInitReal - Without token and null maxSamples")
    void triggerTrainInitReal_WithoutTokenAndNullMaxSamples() {
        ReflectionTestUtils.setField(recommendModelService, "token", null);
        TrainInitResponseDto expectedDto = new TrainInitResponseDto();
        ResponseEntity<TrainInitResponseDto> responseEntity = new ResponseEntity<>(expectedDto, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(pythonApi + "/api/v1/recommendations/train-init-real"),
                eq(HttpMethod.POST),
                eq(HttpEntity.EMPTY),
                eq(TrainInitResponseDto.class)
        )).thenReturn(responseEntity);

        TrainInitResponseDto result = recommendModelService.triggerTrainInitReal(null);

        assertThat(result).isSameAs(expectedDto);
    }

    @Test
    @DisplayName("downloadTrainData - With custom headers from remote")
    void downloadTrainData_WithRemoteHeaders() {
        Resource mockResource = new ByteArrayResource("data".getBytes());
        HttpHeaders remoteHeaders = new HttpHeaders();
        remoteHeaders.setContentType(MediaType.APPLICATION_PDF);
        remoteHeaders.setContentDisposition(ContentDisposition.attachment().filename("custom.pdf").build());

        ResponseEntity<Resource> responseEntity = new ResponseEntity<>(mockResource, remoteHeaders, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(pythonApi + "/api/v1/recommendations/train-data/download"),
                eq(HttpMethod.GET),
                isNull(),
                eq(Resource.class)
        )).thenReturn(responseEntity);

        ResponseEntity<Resource> result = recommendModelService.downloadTrainData();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(mockResource);
        assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(result.getHeaders().getContentDisposition().getFilename()).isEqualTo("custom.pdf");
    }

    @Test
    @DisplayName("downloadTrainData - Without remote headers (default fallback)")
    void downloadTrainData_WithFallbackHeaders() {
        Resource mockResource = new ByteArrayResource("data".getBytes());
        HttpHeaders remoteHeaders = new HttpHeaders(); // empty headers

        ResponseEntity<Resource> responseEntity = new ResponseEntity<>(mockResource, remoteHeaders, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(pythonApi + "/api/v1/recommendations/train-data/download"),
                eq(HttpMethod.GET),
                isNull(),
                eq(Resource.class)
        )).thenReturn(responseEntity);

        ResponseEntity<Resource> result = recommendModelService.downloadTrainData();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(mockResource);
    }
}
