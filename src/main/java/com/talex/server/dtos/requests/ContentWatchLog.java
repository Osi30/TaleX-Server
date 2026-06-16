package com.talex.server.dtos.requests;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ContentWatchLog implements Serializable {
    private String viewerId;
    private String contentId;
    private Long completionTime;
    private Long totalTime;
    private Boolean isLike;
    private Boolean isComment;
    private Boolean isBookmark;
    private Boolean isShare;
    private LocalDateTime timestamp;
}
