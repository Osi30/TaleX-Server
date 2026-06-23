package com.talex.server.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InteractionType {
    LIKE, COMMENT, BOOKMARK, SHARE, UNLIKE, UNBOOKMARK
}
