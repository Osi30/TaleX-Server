package com.talex.server.controllers.recommend;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.recommend.response.TrainInitResponseDto;
import com.talex.server.services.recommend.RecommendModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recommendations/model")
@RequiredArgsConstructor
@Tag(name = "Recommendation Model Management", description = "API quản lý, kích hoạt huấn luyện và tải dữ liệu bộ não đề xuất AI (LightGBM)")
public class RecommendModelController {

    private final RecommendModelService recommendModelService;

    @PostMapping("/train-init")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Khởi tạo & Huấn luyện Model (Mock Data)",
            description = "Gửi yêu cầu sang Python AI Service để kích hoạt tiến trình tạo dữ liệu mẫu và huấn luyện lại bộ não LightGBM. Chỉ Admin."
    )
    public ResponseEntity<BaseResponse> triggerTrainInit(
    ) {
        TrainInitResponseDto result = recommendModelService.triggerTrainInit();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .code(201)
                        .message("Khởi tạo huấn luyện mô hình (Mock Data) thành công!")
                        .data(result)
                        .build());
    }

    @PostMapping("/train-init-real")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Khởi tạo & Huấn luyện Model (Dữ liệu THỰC)",
            description = "Gửi yêu cầu sang Python AI Service để trích xuất dữ liệu thực từ PostgreSQL + MongoDB Atlas và huấn luyện lại bộ não LightGBM. Chỉ Admin."
    )
    public ResponseEntity<BaseResponse> triggerTrainInitReal(
            @RequestParam(defaultValue = "10000") Integer maxSamples
    ) {
        TrainInitResponseDto result = recommendModelService.triggerTrainInitReal(maxSamples);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .code(201)
                        .message("Khởi tạo huấn luyện mô hình từ dữ liệu THỰC thành công!")
                        .data(result)
                        .build());
    }

    @GetMapping("/train-data/download")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Tải file tập dữ liệu huấn luyện (Excel)",
            description = "Tải file train_data.xlsx được xuất từ server Python về máy cục bộ. Chỉ Admin."
    )
    public ResponseEntity<Resource> downloadTrainData() {
        return recommendModelService.downloadTrainData();
    }
}