package com.talex.server.controllers.mongo;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.entities.mongo.SeriesMetadata;
import com.talex.server.services.mongo.SeriesFeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/series-features")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Series Feature API",
        description = "API truy vấn trực tiếp thông tin đặc trưng (features/metadata) của Series trong MongoDB"
)
public class SeriesFeatureController {

    private final SeriesFeatureService seriesFeatureService;

    @GetMapping("/{seriesId}")
    @Operation(
            summary = "Lấy đầy đủ thông tin Series Feature theo Series ID",
            description = "Truy vấn và trả về trực tiếp document SeriesMetadata từ MongoDB dựa vào seriesId truyền trên Path Variable."
    )
    public ResponseEntity<BaseResponse> getSeriesFeatureById(@PathVariable String seriesId) {
        SeriesMetadata seriesFeature = seriesFeatureService.getSeriesFeatureById(seriesId);

        if (seriesFeature == null) {
            return ResponseEntity.ok(BaseResponse.builder()
                    .code(404)
                    .message("Không tìm thấy thông tin Series Feature cho ID: " + seriesId)
                    .data(null)
                    .build());
        }

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy đầy đủ Series Feature thành công!")
                .data(seriesFeature)
                .build());
    }
}