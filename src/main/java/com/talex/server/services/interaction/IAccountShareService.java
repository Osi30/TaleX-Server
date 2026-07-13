package com.talex.server.services.interaction;

import com.talex.server.dtos.interaction.request.ShareRequest;

public interface IAccountShareService {
    void shareEpisode(ShareRequest shareRequest);
}
