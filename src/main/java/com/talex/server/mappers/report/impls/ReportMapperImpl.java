package com.talex.server.mappers.report.impls;

import com.talex.server.dtos.report.request.ReportRequestDto;
import com.talex.server.dtos.report.response.ReportResponseDto;
import com.talex.server.entities.report.Report;
import com.talex.server.mappers.report.ReportMapper;
import org.springframework.stereotype.Component;

@Component
public class ReportMapperImpl implements ReportMapper {

    @Override
    public Report toEntity(ReportRequestDto requestDto) {
        if (requestDto == null) return null;
        Report report = Report.builder()
                .targetType(requestDto.getTargetType())
                .targetId(requestDto.getTargetId())
                .reason(requestDto.getReason())
                .description(requestDto.getDescription())
                .build();

        java.util.List<com.talex.server.entities.report.ReportMedia> mediaList = new java.util.ArrayList<>();
        if (requestDto.getProofImages() != null) {
            for (String url : requestDto.getProofImages()) {
                mediaList.add(com.talex.server.entities.report.ReportMedia.builder()
                        .mediaUrl(url)
                        .mediaType(com.talex.server.enums.report.ReportMediaType.IMAGE)
                        .report(report)
                        .build());
            }
        }
        if (requestDto.getProofVideos() != null) {
            for (String url : requestDto.getProofVideos()) {
                mediaList.add(com.talex.server.entities.report.ReportMedia.builder()
                        .mediaUrl(url)
                        .mediaType(com.talex.server.enums.report.ReportMediaType.VIDEO)
                        .report(report)
                        .build());
            }
        }
        report.setMediaList(mediaList);
        return report;
    }

    @Override
    public ReportResponseDto toResponseDto(Report entity) {
        if (entity == null) return null;

        java.util.List<String> proofImages = new java.util.ArrayList<>();
        java.util.List<String> proofVideos = new java.util.ArrayList<>();

        if (entity.getMediaList() != null) {
            for (com.talex.server.entities.report.ReportMedia media : entity.getMediaList()) {
                if (com.talex.server.enums.report.ReportMediaType.IMAGE.equals(media.getMediaType())) {
                    proofImages.add(media.getMediaUrl());
                } else if (com.talex.server.enums.report.ReportMediaType.VIDEO.equals(media.getMediaType())) {
                    proofVideos.add(media.getMediaUrl());
                }
            }
        }

        return ReportResponseDto.builder()
                .reportId(entity.getReportId())
                .reporterId(entity.getReporterId())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .reason(entity.getReason())
                .description(entity.getDescription())
                .proofImages(proofImages.isEmpty() ? null : proofImages)
                .proofVideos(proofVideos.isEmpty() ? null : proofVideos)
                .status(entity.getStatus())
                .ticketId(entity.getModerationTicket() != null ? entity.getModerationTicket().getTicketId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}