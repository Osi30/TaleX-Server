package com.talex.server.services.series;

import com.talex.server.dtos.requests.series.ComboEpisodeRequestDto;
import com.talex.server.dtos.responses.series.ComboEpisodeResponseDto;

import java.util.List;

public interface ComboEpisodeService {
    ComboEpisodeResponseDto create(ComboEpisodeRequestDto request, String accountId);
    ComboEpisodeResponseDto getById(String id, String accountId);
    List<ComboEpisodeResponseDto> listByCreator(String accountId);
    List<ComboEpisodeResponseDto> getAll();
    ComboEpisodeResponseDto update(String id, ComboEpisodeRequestDto request, String accountId);
    void delete(String id, String accountId);
}
