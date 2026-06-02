package com.talex.server.services;

import com.talex.server.dtos.responses.GoogleUserInfo;

public interface GoogleAuthService {

    GoogleUserInfo verifyIdToken(String idToken);
}
