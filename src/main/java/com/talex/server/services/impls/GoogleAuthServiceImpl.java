package com.talex.server.services.impls;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.talex.server.dtos.responses.GoogleUserInfo;
import com.talex.server.exceptions.UnauthorizedException;
import com.talex.server.services.GoogleAuthService;
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
                throw new UnauthorizedException("Invalid Google ID token");
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
            throw new UnauthorizedException("Failed to verify Google token");
        }
    }
}
