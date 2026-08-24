package com.talex.server.services.interaction.impls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.interaction.request.WatchTimeRequest;
import com.talex.server.dtos.interaction.response.WatchSessionResponseDto;
import com.talex.server.dtos.responses.series.EpisodeResponseDto;
import com.talex.server.entities.interaction.WatchSession;
import com.talex.server.entities.series.Episode;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.repositories.interaction.WatchSessionRepository;
import com.talex.server.services.series.EpisodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WatchSessionService Tests")
class WatchSessionServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private WatchSessionRepository watchSessionRepository;
    @Mock
    private EpisodeService episodeService;

    @InjectMocks
    private WatchSessionService service;

    private UUID accountId;
    private String episodeId;
    private String sessionId;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        episodeId = "ep-1";
        sessionId = "session-1";
    }

    @Test
    @DisplayName("sendWatchHeartbeat - Success with null fallbacks vs non-null parameters")
    void sendWatchHeartbeat_Success() {
        // Case 1: Non-null params & non-null heartbeatValue
        WatchTimeRequest req1 = new WatchTimeRequest();
        req1.setSessionId(sessionId);
        req1.setEpisodeId(episodeId);
        req1.setCurrentPosition(100.0);
        req1.setHeartbeatValue(5.0);
        req1.setEvent("PROGRESS");

        service.sendWatchHeartbeat(req1, accountId, "127.0.0.1");
        verify(kafkaTemplate).send(eq("watch-raw"), eq(episodeId), anyString());

        // Case 2: Null accountId, null ipAddress, null heartbeatValue
        WatchTimeRequest req2 = new WatchTimeRequest();
        req2.setSessionId(sessionId);
        req2.setEpisodeId(episodeId);
        req2.setCurrentPosition(105.0);
        req2.setHeartbeatValue(null);
        req2.setEvent("PROGRESS");

        service.sendWatchHeartbeat(req2, null, null);
        verify(kafkaTemplate, times(2)).send(eq("watch-raw"), eq(episodeId), anyString());
    }

    @Test
    @DisplayName("sendWatchHeartbeat - Exception during processing throws KAFKA_PROCESSING_ERROR")
    void sendWatchHeartbeat_Exception() throws Exception {
        WatchTimeRequest req = new WatchTimeRequest();
        req.setSessionId(sessionId);
        req.setEpisodeId(episodeId);
        req.setCurrentPosition(100.0);
        req.setEvent("PROGRESS");

        ObjectMapper mockMapper = mock(ObjectMapper.class);
        WatchSessionService customService = new WatchSessionService(
                kafkaTemplate, mockMapper, watchSessionRepository, episodeService);

        when(mockMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization failed"));

        assertThatThrownBy(() -> customService.sendWatchHeartbeat(req, accountId, "127.0.0.1"))
                .isInstanceOf(InteractionException.class)
                .extracting("errorCode")
                .isEqualTo(InteractionErrorCode.KAFKA_PROCESSING_ERROR);
    }

    @Test
    @DisplayName("getRecentWatchSessions - Maps Slice of WatchSession to DTOs")
    void getRecentWatchSessions() {
        Pageable pageable = PageRequest.of(0, 10);

        Episode ep = new Episode();
        ep.setEpisodeId(episodeId);

        EpisodeResponseDto epRes = EpisodeResponseDto.builder().episodeId(episodeId).title("Ep 1").build();
        when(episodeService.toResponse(ep)).thenReturn(epRes);

        WatchSession session = new WatchSession();
        session.setId("ws-1");
        session.setEpisode(ep);
        session.setWatchDuration(120.0);
        session.setHeartbeatCount(24);
        session.setStartTime(LocalDateTime.now());
        session.setEndTime(LocalDateTime.now());
        session.setCurrentPosition(120.0);
        session.setUpdatedAt(LocalDateTime.now());

        Slice<WatchSession> slice = new SliceImpl<>(List.of(session), pageable, false);
        when(watchSessionRepository.findLatestWatchSessionsByAccountId(accountId, pageable)).thenReturn(slice);

        Slice<WatchSessionResponseDto> res = service.getRecentWatchSessions(accountId, pageable);

        assertThat(res.getContent()).hasSize(1);
        assertThat(res.getContent().get(0).getId()).isEqualTo("ws-1");
        assertThat(res.getContent().get(0).getEpisode().getEpisodeId()).isEqualTo(episodeId);
        assertThat(res.getContent().get(0).getWatchDuration()).isEqualTo(120.0);
    }
}
