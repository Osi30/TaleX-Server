package com.talex.server.services.interaction;

import com.talex.server.dtos.interaction.request.ViewRequest;

public interface ViewService {
    void viewEpisode(ViewRequest request);
}