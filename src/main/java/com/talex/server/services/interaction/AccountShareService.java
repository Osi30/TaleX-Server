package com.talex.server.services.interaction;

import com.talex.server.dtos.interaction.request.ShareRequest;

public interface AccountShareService {
    void shareEpisode(ShareRequest shareRequest);
}
