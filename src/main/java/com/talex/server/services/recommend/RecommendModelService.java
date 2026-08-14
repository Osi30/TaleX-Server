package com.talex.server.services.recommend;

import com.talex.server.dtos.recommend.response.TrainInitResponseDto;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public interface RecommendModelService {

    /**
     * Khởi tạo và huấn luyện mô hình bằng dữ liệu giả lập (Mock Data)
     */
    TrainInitResponseDto triggerTrainInit();

    /**
     * Khởi tạo và huấn luyện mô hình bằng dữ liệu THỰC (PostgreSQL + MongoDB)
     */
    TrainInitResponseDto triggerTrainInitReal(Integer maxSamples);

    /**
     * Tải file dataset huấn luyện dạng Excel từ Python Server
     */
    ResponseEntity<Resource> downloadTrainData();
}