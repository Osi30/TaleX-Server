package com.talex.server.services;

import com.talex.server.dtos.requests.ComboEpisodeRequestDto;
import com.talex.server.dtos.responses.ComboEpisodeResponseDto;

import java.util.List;

public interface ComboEpisodeService {
    ComboEpisodeResponseDto create(ComboEpisodeRequestDto request, String accountId);
    ComboEpisodeResponseDto getById(String id, String accountId);
    List<ComboEpisodeResponseDto> listByCreator(String accountId);
    ComboEpisodeResponseDto update(String id, ComboEpisodeRequestDto request, String accountId);
    void delete(String id, String accountId);
}
