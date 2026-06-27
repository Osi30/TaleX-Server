package com.talex.server.services;

import com.talex.server.entities.Episode;
import com.talex.server.entities.Season;
import com.talex.server.entities.Series;
import com.talex.server.entities.creator.Creator;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.services.creator.ICreatorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentOwnershipServiceTest {
    private static final UUID ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String CREATOR_ID = "creator-1";

    @Mock
    private ICreatorService creatorService;

    @InjectMocks
    private ContentOwnershipService service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void creatorCanManageOwnedContentHierarchy() {
        authenticateAs("ROLE_CREATOR");
        mockCurrentCreator(CREATOR_ID);
        Episode episode = episodeOwnedBy(CREATOR_ID);

        assertDoesNotThrow(() -> service.assertCanManage(episode, ACCOUNT_ID.toString()));
        assertDoesNotThrow(() -> service.assertCanManage(episode.getSeason(), ACCOUNT_ID.toString()));
        assertDoesNotThrow(() -> service.assertCanManage(episode.getSeason().getSeries(), ACCOUNT_ID.toString()));
    }

    @Test
    void creatorCannotManageAnotherCreatorsEpisode() {
        authenticateAs("ROLE_CREATOR");
        mockCurrentCreator("creator-2");
        Episode episode = episodeOwnedBy(CREATOR_ID);

        ContentModuleException exception = assertThrows(
                ContentModuleException.class,
                () -> service.assertCanManage(episode, ACCOUNT_ID.toString()));

        assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        assertEquals(4403, exception.getCode());
    }

    @Test
    void creatorCannotManageContentWithInconsistentOwnershipChain() {
        authenticateAs("ROLE_CREATOR");
        mockCurrentCreator(CREATOR_ID);
        Episode episode = episodeOwnedBy(CREATOR_ID);
        episode.getSeason().setCreatorId("creator-2");

        assertThrows(
                ContentModuleException.class,
                () -> service.assertCanManage(episode, ACCOUNT_ID.toString()));
    }

    @Test
    void staffAndAdminCanManageAnyContentWithoutCreatorLookup() {
        Episode episode = episodeOwnedBy(CREATOR_ID);

        for (String role : List.of("ROLE_STAFF", "ROLE_ADMIN")) {
            authenticateAs(role);
            assertDoesNotThrow(() -> service.assertCanManage(episode, "staff-or-admin-account"));
        }
    }

    private void mockCurrentCreator(String creatorId) {
        Creator creator = new Creator();
        creator.setCreatorId(creatorId);
        when(creatorService.getEntityByAccountId(ACCOUNT_ID)).thenReturn(creator);
    }

    private void authenticateAs(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        ACCOUNT_ID.toString(),
                        null,
                        List.of(new SimpleGrantedAuthority(role))));
    }

    private Episode episodeOwnedBy(String creatorId) {
        Series series = new Series();
        series.setCreatorId(creatorId);

        Season season = new Season();
        season.setSeries(series);
        season.setCreatorId(creatorId);

        Episode episode = new Episode();
        episode.setSeason(season);
        episode.setCreatorId(creatorId);
        return episode;
    }
}