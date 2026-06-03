package com.talex.server.configs;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Stream;

@Configuration
public class GoogleOAuthConfig {

    @Value("${google.client-id.web:}")
    private String webClientId;

    @Value("${google.client-id.android:}")
    private String androidClientId;

    @Value("${google.client-id.ios:}")
    private String iosClientId;

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier() {
        // Collect all non-empty client IDs into the audience list
        List<String> audience = Stream.of(webClientId, androidClientId, iosClientId)
                .filter(id -> id != null && !id.isBlank())
                .toList();

        return new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(audience)
                .build();
    }
}
