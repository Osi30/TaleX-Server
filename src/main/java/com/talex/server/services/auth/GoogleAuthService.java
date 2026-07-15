package com.talex.server.services.auth;

import com.talex.server.dtos.responses.auth.GoogleUserInfo;

public interface GoogleAuthService {

    GoogleUserInfo verifyIdToken(String idToken);
}
