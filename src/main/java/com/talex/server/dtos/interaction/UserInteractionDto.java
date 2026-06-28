package com.talex.server.dtos.interaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInteractionDto {
    private String accountId;
    private String episodeId;
    private boolean isLike;
    private boolean isBookmark;
    private boolean hasLikeChange = false;
    private boolean hasBookmarkChange = false;

    public UserInteractionDto(String accountId, String episodeId) {
        this.accountId = accountId;
        this.episodeId = episodeId;
    }
}