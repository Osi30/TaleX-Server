package com.talex.server.dtos.interaction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.UUID;

@Data
public class FollowRequestDto {
    @JsonIgnore
    private UUID followerId;
    private UUID followedId;
}
