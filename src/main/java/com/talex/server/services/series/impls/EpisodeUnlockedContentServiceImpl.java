package com.talex.server.services.series.impls;

import com.talex.server.entities.auth.Account;
import com.talex.server.entities.series.ComboEpisode;
import com.talex.server.entities.series.Episode;
import com.talex.server.entities.series.EpisodeUnlockedContent;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.repositories.series.ComboEpisodeRepository;
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.repositories.series.EpisodeUnlockedContentRepository;
import com.talex.server.repositories.transaction.OrderRepository;
import com.talex.server.entities.transaction.Order;
import com.talex.server.services.series.EpisodeUnlockedContentService;
import com.talex.server.services.audit.ContentAuditLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EpisodeUnlockedContentServiceImpl implements EpisodeUnlockedContentService {

    private final EpisodeUnlockedContentRepository episodeUnlockedContentRepository;
    private final EpisodeRepository episodeRepository;
    private final ComboEpisodeRepository comboEpisodeRepository;
    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;
    private final ContentAuditLogger contentAuditLogger;

    @Override
    @Transactional
    public List<EpisodeUnlockedContent> createFromOrder(String orderId, String itemId, String itemType, UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        
        if (!order.getAccount().getAccountId().equals(accountId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order does not belong to the current user");
        }

        List<EpisodeUnlockedContent> unlockedContents = new ArrayList<>();

        if ("EPISODE".equalsIgnoreCase(itemType)) {
            Episode episode = episodeRepository.findById(itemId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Episode not found"));
            
            if (!episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(accountId, episode.getEpisodeId())) {
                unlockedContents.add(createUnlockedContent(account, episode, orderId, episode.getPriceVnd()));
            }

        } else if ("COMBO".equalsIgnoreCase(itemType)) {
            ComboEpisode combo = comboEpisodeRepository.findById(itemId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Combo not found"));
            
            if (combo.getIsDeleted()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Combo is deleted");
            }

            if (combo.getEpisodes() != null) {
                // Distribute combo price to episodes, or just record the episode's original price
                // For simplicity, we record the episode's original price.
                for (Episode episode : combo.getEpisodes()) {
                    if (!episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(accountId, episode.getEpisodeId())) {
                        unlockedContents.add(createUnlockedContent(account, episode, orderId, episode.getPriceVnd()));
                    }
                }
            }

        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid itemType. Must be EPISODE or COMBO");
        }

        if (!unlockedContents.isEmpty()) {
            List<EpisodeUnlockedContent> saved = episodeUnlockedContentRepository.saveAll(unlockedContents);
            for (EpisodeUnlockedContent content : saved) {
                contentAuditLogger.logAction("EpisodeUnlockedContent", content.getId().toString(), "CREATE", accountId.toString(), content.getEpisode().getCreatorId());
            }
            return saved;
        }

        return unlockedContents;
    }

    private EpisodeUnlockedContent createUnlockedContent(Account account, Episode episode, String orderId, Long price) {
        return EpisodeUnlockedContent.builder()
                .account(account)
                .episode(episode)
                .orderId(orderId)
                .purchasePriceVnd(price)
                .unlockMethod("ORDER")
                .build();
    }
}
