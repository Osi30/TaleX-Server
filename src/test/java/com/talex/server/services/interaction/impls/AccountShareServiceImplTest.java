package com.talex.server.services.interaction.impls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.interaction.request.ShareRequest;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountShareServiceImpl Tests")
class AccountShareServiceImplTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AccountShareServiceImpl service;

    private UUID accountId;
    private String episodeId;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        episodeId = "ep-50";
    }

    @Test
    @DisplayName("shareEpisode - Blank episodeId throws exception")
    void shareEpisode_BlankEpisodeId() {
        ShareRequest request = new ShareRequest();
        request.setEpisodeId("  ");
        request.setAccountId(accountId);

        assertThatThrownBy(() -> service.shareEpisode(request))
                .isInstanceOf(InteractionException.class)
                .extracting("errorCode")
                .isEqualTo(InteractionErrorCode.SAVING_DATABASE_ERROR);
    }

    @Test
    @DisplayName("shareEpisode - Success flow with null vs non-null accountId")
    void shareEpisode_Success() {
        // Non-null accountId
        ShareRequest req1 = new ShareRequest();
        req1.setEpisodeId(episodeId);
        req1.setAccountId(accountId);
        req1.setIpAddress("127.0.0.1");

        service.shareEpisode(req1);
        verify(kafkaTemplate).send(eq("talex-interaction.episode-shared"), eq(episodeId), anyString());

        // Null accountId
        ShareRequest req2 = new ShareRequest();
        req2.setEpisodeId(episodeId);
        req2.setAccountId(null);
        req2.setIpAddress("127.0.0.1");

        service.shareEpisode(req2);
        verify(kafkaTemplate, times(2)).send(eq("talex-interaction.episode-shared"), eq(episodeId), anyString());
    }

    @Test
    @DisplayName("shareEpisode - Exception in serialization/Kafka throws KAFKA_PROCESSING_ERROR")
    void shareEpisode_Exception() throws Exception {
        ShareRequest req = new ShareRequest();
        req.setEpisodeId(episodeId);
        req.setAccountId(accountId);
        req.setIpAddress("127.0.0.1");

        ObjectMapper mockMapper = mock(ObjectMapper.class);
        AccountShareServiceImpl customService = new AccountShareServiceImpl(kafkaTemplate, mockMapper);

        when(mockMapper.writeValueAsString(any())).thenThrow(new RuntimeException("JSON serialization error"));

        assertThatThrownBy(() -> customService.shareEpisode(req))
                .isInstanceOf(InteractionException.class)
                .extracting("errorCode")
                .isEqualTo(InteractionErrorCode.KAFKA_PROCESSING_ERROR);
    }
}
