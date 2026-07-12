package com.talex.server.services.interaction;

import com.talex.server.dtos.interaction.request.ViewRequest;

import java.util.UUID;

public interface IEpisodeViewService {
    void viewEpisode(ViewRequest request);
}