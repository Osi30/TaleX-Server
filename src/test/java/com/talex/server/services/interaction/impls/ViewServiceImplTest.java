package com.talex.server.services.interaction.impls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.interaction.request.ViewRequest;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ViewServiceImpl Tests")
class ViewServiceImplTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ViewServiceImpl service;

    private UUID accountId;
    private String episodeId;
    private String sessionId;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        episodeId = "ep-1";
        sessionId = "session-999";
    }

    @Test
    @DisplayName("viewEpisode - Success flow with null vs non-null accountId and ipAddress")
    void viewEpisode_Success() {
        // Non-null accountId and ipAddress
        ViewRequest req1 = new ViewRequest();
        req1.setEpisodeId(episodeId);
        req1.setAccountId(accountId);
        req1.setSessionId(sessionId);
        req1.setIpAddress("192.168.1.1");

        service.viewEpisode(req1);
        verify(kafkaTemplate).send(eq("talex-interaction.episode-viewed"), eq(sessionId), anyString());

        // Null accountId and null ipAddress
        ViewRequest req2 = new ViewRequest();
        req2.setEpisodeId(episodeId);
        req2.setAccountId(null);
        req2.setSessionId(sessionId);
        req2.setIpAddress(null);

        service.viewEpisode(req2);
        verify(kafkaTemplate, times(2)).send(eq("talex-interaction.episode-viewed"), eq(sessionId), anyString());
    }

    @Test
    @DisplayName("viewEpisode - Exception during processing throws KAFKA_PROCESSING_ERROR")
    void viewEpisode_Exception() throws Exception {
        ViewRequest req = new ViewRequest();
        req.setEpisodeId(episodeId);
        req.setSessionId(sessionId);

        ObjectMapper mockMapper = mock(ObjectMapper.class);
        ViewServiceImpl customService = new ViewServiceImpl(kafkaTemplate, mockMapper);

        when(mockMapper.writeValueAsString(any())).thenThrow(new RuntimeException("JSON serialization error"));

        assertThatThrownBy(() -> customService.viewEpisode(req))
                .isInstanceOf(InteractionException.class)
                .extracting("errorCode")
                .isEqualTo(InteractionErrorCode.KAFKA_PROCESSING_ERROR);
    }
}
