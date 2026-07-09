package com.talex.server.services.interaction;

import com.talex.server.dtos.interaction.response.AccountFollowInfoDto;
import com.talex.server.dtos.interaction.request.FollowRequestDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.UUID;

public interface IAccountFollowService {
    void follow(FollowRequestDto request);

    void unfollow(FollowRequestDto request);

    Slice<AccountFollowInfoDto> getFollowers(UUID accountId, Pageable pageable);

    Slice<AccountFollowInfoDto> getFollowed(UUID accountId, Pageable pageable);
}
