package com.talex.server.services.interaction;

import com.talex.server.dtos.interaction.request.ViewRequest;

public interface IViewService {
    void viewEpisode(ViewRequest request);
}