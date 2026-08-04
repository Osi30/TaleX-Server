package com.talex.server.dtos.responses.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleUserInfo {
    private String email;
    private String googleSubId;
    private String name;
    private String pictureUrl;
    /** Claim `email_verified` từ Google ID token — null/false nghĩa là CHƯA verified. */
    private Boolean emailVerified;
}
