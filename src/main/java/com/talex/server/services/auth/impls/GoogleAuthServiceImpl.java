package com.talex.server.services.auth.impls;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.talex.server.dtos.responses.auth.GoogleUserInfo;
import com.talex.server.exceptions.codes.AuthErrorCode;
import com.talex.server.exceptions.details.AuthException;
import com.talex.server.services.auth.GoogleAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthServiceImpl implements GoogleAuthService {

    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    @Override
    public GoogleUserInfo verifyIdToken(String idToken) {
        try {
            GoogleIdToken token = googleIdTokenVerifier.verify(idToken);

            if (token == null) {
                log.warn("Google token verification failed");
                throw new AuthException(AuthErrorCode.INVALID_GOOGLE_TOKEN);
            }

            GoogleIdToken.Payload payload = token.getPayload();
            String email = payload.getEmail();

            log.info("Google token verified: {}", email);

            return GoogleUserInfo.builder()
                    .email(email)
                    .googleSubId(payload.getSubject())
                    .name((String) payload.get("name"))
                    .pictureUrl((String) payload.get("picture"))
                    .build();

        } catch (GeneralSecurityException | IOException e) {
            log.error("Google token verification error", e);
            throw new AuthException(AuthErrorCode.INVALID_GOOGLE_TOKEN,
                    "Failed to verify Google token", e);
        }
    }
}
