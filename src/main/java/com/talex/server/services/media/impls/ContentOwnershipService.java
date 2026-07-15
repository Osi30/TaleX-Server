package com.talex.server.services.media.impls;

import com.talex.server.entities.media.Media;
import com.talex.server.entities.series.Episode;
import com.talex.server.entities.series.Season;
import com.talex.server.entities.series.Series;
import com.talex.server.entities.series.ComboEpisode;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.services.creator.ICreatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContentOwnershipService {
    private static final Set<String> PRIVILEGED_ROLES = Set.of("ROLE_STAFF", "ROLE_ADMIN");

    private final ICreatorService creatorService;

    public void assertCanView(Media media, String accountId) {
        if (isPrivileged()) return;
        assertOwnedByCreator(media, requireCurrentCreatorId(accountId));
    }

    public void assertCanView(Series series, String accountId) {
        if (isPrivileged()) return;
        assertOwnerIds(requireCurrentCreatorId(accountId), series.getCreator().getCreatorId());
    }

    public void assertCanView(Season season, String accountId) {
        if (isPrivileged()) return;
        assertOwnedByCreator(season, requireCurrentCreatorId(accountId));
    }

    public void assertCanView(Episode episode, String accountId) {
        if (isPrivileged()) return;
        assertOwnedByCreator(episode, requireCurrentCreatorId(accountId));
    }

    public void assertCanView(ComboEpisode combo, String accountId) {
        if (isPrivileged()) return;
        assertOwnedByCreator(combo, requireCurrentCreatorId(accountId));
    }

    public void assertCanManage(Media media, String accountId) {
        assertOwnedByCreator(media, requireCurrentCreatorId(accountId));
        
        Episode episode = media.getEpisode();
        if (media.getStatus() == com.talex.server.enums.media.MediaStatus.FORCE_HIDDEN ||
            episode.getStatus() == com.talex.server.enums.series.EpisodeStatus.FORCE_HIDDEN ||
            episode.getSeason().getStatus() == com.talex.server.enums.series.SeasonStatus.FORCE_HIDDEN ||
            episode.getSeason().getSeries().getStatus() == com.talex.server.enums.series.SeriesStatus.FORCE_HIDDEN) {
            throw ContentModuleException.forbidden("Content is force-hidden by admin and cannot be modified.");
        }
    }

    public void assertOwnedByCreator(Media media, String creatorId) {
        assertOwnerIds(
                creatorId,
                media.getCreatorId());
    }

    public void assertCanManage(Series series, String accountId) {
        assertOwnerIds(requireCurrentCreatorId(accountId), series.getCreator().getCreatorId());
        
        if (series.getStatus() == com.talex.server.enums.series.SeriesStatus.FORCE_HIDDEN) {
            throw ContentModuleException.forbidden("Content is force-hidden by admin and cannot be modified.");
        }
    }

    public void assertCanManage(Season season, String accountId) {
        assertOwnedByCreator(season, requireCurrentCreatorId(accountId));
        
        if (season.getStatus() == com.talex.server.enums.series.SeasonStatus.FORCE_HIDDEN ||
            season.getSeries().getStatus() == com.talex.server.enums.series.SeriesStatus.FORCE_HIDDEN) {
            throw ContentModuleException.forbidden("Content is force-hidden by admin and cannot be modified.");
        }
    }

    public void assertOwnedByCreator(Season season, String creatorId) {
        assertOwnerIds(
                creatorId,
                season.getSeries().getCreator().getCreatorId());
    }

    public void assertCanManage(Episode episode, String accountId) {
        assertOwnedByCreator(episode, requireCurrentCreatorId(accountId));
        
        if (episode.getStatus() == com.talex.server.enums.series.EpisodeStatus.FORCE_HIDDEN ||
            episode.getSeason().getStatus() == com.talex.server.enums.series.SeasonStatus.FORCE_HIDDEN ||
            episode.getSeason().getSeries().getStatus() == com.talex.server.enums.series.SeriesStatus.FORCE_HIDDEN) {
            throw ContentModuleException.forbidden("Content is force-hidden by admin and cannot be modified.");
        }
    }

    public void assertOwnedByCreator(Episode episode, String creatorId) {
        assertOwnerIds(
                creatorId,
                episode.getCreatorId());
    }

    public void assertCanManage(ComboEpisode combo, String accountId) {
        assertOwnedByCreator(combo, requireCurrentCreatorId(accountId));
    }

    public void assertOwnedByCreator(ComboEpisode combo, String creatorId) {
        assertOwnerIds(
                creatorId,
                combo.getCreatorId());
    }

    private void assertOwnerIds(String currentCreatorId, String... ownerCreatorIds) {
        for (String ownerCreatorId : ownerCreatorIds) {
            if (!currentCreatorId.equals(ownerCreatorId)) {
                throwForbidden();
            }
        }
    }

    private void throwForbidden() {
        throw ContentModuleException.forbidden(
                "You do not have permission to manage this content");
    }

    public boolean isPrivileged() {
        return PRIVILEGED_ROLES.stream().anyMatch(this::hasRole);
    }

    public String requireCurrentCreatorId(String accountId) {
        if ((!hasRole("ROLE_CREATOR") && !hasRole("ROLE_VIEWER")) || accountId == null) {
            throwForbidden();
        }
        return creatorService
                .getEntityByAccountId(UUID.fromString(accountId))
                .getCreatorId();
    }

    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}
