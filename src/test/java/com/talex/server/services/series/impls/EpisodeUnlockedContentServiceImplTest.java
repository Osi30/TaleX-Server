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
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.services.audit.ContentAuditLogger;
import com.talex.server.services.payment.impls.AccountFulfillmentLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EpisodeUnlockedContentService Tests")
class EpisodeUnlockedContentServiceImplTest {

    @Mock
    private EpisodeUnlockedContentRepository episodeUnlockedContentRepository;

    @Mock
    private EpisodeRepository episodeRepository;

    @Mock
    private ComboEpisodeRepository comboEpisodeRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ContentAuditLogger contentAuditLogger;

    @Mock
    private AccountFulfillmentLock accountFulfillmentLock;

    @InjectMocks
    private EpisodeUnlockedContentServiceImpl service;

    private UUID testAccountId;
    private String testOrderId;
    private String testEpisodeId;
    private String testComboId;
    private Account testAccount;
    private Order testOrder;
    private Episode testEpisode;
    private ComboEpisode testCombo;

    // Episode/ComboEpisode KHÔNG có @Builder (chỉ @Getter/@Setter/@NoArgsConstructor) —
    // dùng helper thay vì .builder() để khớp entity thật.
    private static Episode newEpisode(String episodeId, String creatorId, String title, Long priceVnd) {
        Episode episode = new Episode();
        episode.setEpisodeId(episodeId);
        episode.setCreatorId(creatorId);
        episode.setTitle(title);
        episode.setPriceVnd(priceVnd);
        return episode;
    }

    private static ComboEpisode newCombo(String comboId, String creatorId, String title, List<Episode> episodes) {
        ComboEpisode combo = new ComboEpisode();
        combo.setComboId(comboId);
        combo.setCreatorId(creatorId);
        combo.setTitle(title);
        combo.setEpisodes(episodes);
        return combo;
    }

    @BeforeEach
    void setUp() {
        testAccountId = UUID.randomUUID();
        testOrderId = UUID.randomUUID().toString();
        testEpisodeId = UUID.randomUUID().toString();
        testComboId = UUID.randomUUID().toString();

        // Setup test account
        testAccount = Account.builder()
                .accountId(testAccountId)
                .username("testuser")
                .email("test@example.com")
                .build();

        // Setup test order
        testOrder = Order.builder()
                .orderId(testOrderId)
                .account(testAccount)
                .status(OrderStatus.COMPLETED)
                .totalAmount(BigDecimal.valueOf(100000))
                .coinAmount(0L)
                .fiatAmount(BigDecimal.valueOf(100000))
                .itemType("EPISODE")
                .itemId(testEpisodeId)
                .createdAt(LocalDateTime.now())
                .build();

        // Setup test episode
        testEpisode = newEpisode(testEpisodeId, UUID.randomUUID().toString(), "Test Episode", 50000L);
    }

    // ============= UTCID01: EPISODE valid, not yet unlocked =============
    @Test
    @DisplayName("UTCID01 - itemType=EPISODE, valid account/order/episode, not yet unlocked")
    void testCreateFromOrderEpisodeSuccess() {
        // Arrange
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
        when(episodeRepository.findById(testEpisodeId)).thenReturn(Optional.of(testEpisode));
        when(episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(testAccountId, testEpisodeId))
                .thenReturn(false);

        EpisodeUnlockedContent unlockedContent = EpisodeUnlockedContent.builder()
                .id(UUID.randomUUID())
                .account(testAccount)
                .episode(testEpisode)
                .orderId(testOrderId)
                .purchasePriceVnd(50000L)
                .unlockMethod("ORDER")
                .build();

        when(episodeUnlockedContentRepository.saveAll(any(List.class))).thenReturn(List.of(unlockedContent));

        // Act
        List<EpisodeUnlockedContent> result = service.createFromOrder(testOrderId, testEpisodeId, "EPISODE", testAccountId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccount()).isEqualTo(testAccount);
        assertThat(result.get(0).getEpisode()).isEqualTo(testEpisode);
        assertThat(result.get(0).getOrderId()).isEqualTo(testOrderId);
        assertThat(result.get(0).getUnlockMethod()).isEqualTo("ORDER");
        verify(accountFulfillmentLock, times(1)).acquire(testAccountId);
        verify(episodeUnlockedContentRepository, times(1)).saveAll(any(List.class));
        verify(contentAuditLogger, times(1)).logAction(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // ============= UTCID02: COMBO valid, episodes not unlocked =============
    @Test
    @DisplayName("UTCID02 - itemType=COMBO, valid account/order/combo with episodes, none unlocked yet")
    void testCreateFromOrderComboSuccess() {
        // Arrange
        String comboId = UUID.randomUUID().toString();
        Episode episode1 = newEpisode(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "Combo Episode 1", 30000L);
        Episode episode2 = newEpisode(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "Combo Episode 2", 40000L);

        ComboEpisode combo = newCombo(comboId, UUID.randomUUID().toString(), "Test Combo", List.of(episode1, episode2));

        Order comboOrder = Order.builder()
                .orderId(testOrderId)
                .account(testAccount)
                .status(OrderStatus.COMPLETED)
                .itemType("COMBO")
                .itemId(comboId)
                .build();

        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(comboOrder));
        when(comboEpisodeRepository.findById(comboId)).thenReturn(Optional.of(combo));
        when(episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(testAccountId, episode1.getEpisodeId()))
                .thenReturn(false);
        when(episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(testAccountId, episode2.getEpisodeId()))
                .thenReturn(false);

        EpisodeUnlockedContent unlockedContent1 = EpisodeUnlockedContent.builder()
                .id(UUID.randomUUID())
                .account(testAccount)
                .episode(episode1)
                .orderId(testOrderId)
                .purchasePriceVnd(30000L)
                .unlockMethod("ORDER")
                .build();
        EpisodeUnlockedContent unlockedContent2 = EpisodeUnlockedContent.builder()
                .id(UUID.randomUUID())
                .account(testAccount)
                .episode(episode2)
                .orderId(testOrderId)
                .purchasePriceVnd(40000L)
                .unlockMethod("ORDER")
                .build();

        when(episodeUnlockedContentRepository.saveAll(any(List.class)))
                .thenReturn(List.of(unlockedContent1, unlockedContent2));

        // Act
        List<EpisodeUnlockedContent> result = service.createFromOrder(testOrderId, comboId, "COMBO", testAccountId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(EpisodeUnlockedContent::getOrderId).allMatch(id -> id.equals(testOrderId));
        assertThat(result).extracting(EpisodeUnlockedContent::getUnlockMethod).allMatch(method -> method.equals("ORDER"));
        verify(accountFulfillmentLock, times(1)).acquire(testAccountId);
        verify(episodeUnlockedContentRepository, times(1)).saveAll(any(List.class));
        verify(contentAuditLogger, times(2)).logAction(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // ============= UTCID03: Account not found (404) =============
    @Test
    @DisplayName("UTCID03 - account not found")
    void testCreateFromOrderAccountNotFound() {
        // Arrange
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createFromOrder(testOrderId, testEpisodeId, "EPISODE", testAccountId));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).contains("Account not found");
        verify(accountFulfillmentLock, times(1)).acquire(testAccountId);
        verify(accountRepository, times(1)).findById(testAccountId);
    }

    // ============= UTCID04: Order not found (404) =============
    @Test
    @DisplayName("UTCID04 - order not found")
    void testCreateFromOrderOrderNotFound() {
        // Arrange
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createFromOrder(testOrderId, testEpisodeId, "EPISODE", testAccountId));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).contains("Order not found");
        verify(accountRepository, times(1)).findById(testAccountId);
        verify(orderRepository, times(1)).findById(testOrderId);
    }

    // ============= UTCID05: Order belongs to different account (403) =============
    @Test
    @DisplayName("UTCID05 - order exists but belongs to a different account")
    void testCreateFromOrderOrderBelongsToDifferentAccount() {
        // Arrange
        UUID differentAccountId = UUID.randomUUID();
        Account differentAccount = Account.builder()
                .accountId(differentAccountId)
                .username("otheruser")
                .email("other@example.com")
                .build();

        Order orderBelongingToOther = Order.builder()
                .orderId(testOrderId)
                .account(differentAccount)
                .status(OrderStatus.COMPLETED)
                .build();

        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(orderBelongingToOther));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createFromOrder(testOrderId, testEpisodeId, "EPISODE", testAccountId));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exception.getReason()).contains("Order does not belong to the current user");
        verify(orderRepository, times(1)).findById(testOrderId);
    }

    // ============= UTCID06: Episode not found (404) =============
    @Test
    @DisplayName("UTCID06 - itemType=EPISODE, episode not found")
    void testCreateFromOrderEpisodeNotFound() {
        // Arrange
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
        when(episodeRepository.findById(testEpisodeId)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createFromOrder(testOrderId, testEpisodeId, "EPISODE", testAccountId));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).contains("Episode not found");
        verify(episodeRepository, times(1)).findById(testEpisodeId);
    }

    // ============= UTCID07: Episode already unlocked (race condition) =============
    @Test
    @DisplayName("UTCID07 - itemType=EPISODE, episode already unlocked (race, duplicate)")
    void testCreateFromOrderEpisodeAlreadyUnlocked() {
        // Arrange
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
        when(episodeRepository.findById(testEpisodeId)).thenReturn(Optional.of(testEpisode));
        // Episode already unlocked
        when(episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(testAccountId, testEpisodeId))
                .thenReturn(true);

        // Act
        List<EpisodeUnlockedContent> result = service.createFromOrder(testOrderId, testEpisodeId, "EPISODE", testAccountId);

        // Assert
        assertThat(result).isEmpty();
        verify(episodeUnlockedContentRepository, never()).saveAll(any(List.class));
        verify(contentAuditLogger, never()).logAction(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // ============= UTCID08: Combo not found (404) =============
    @Test
    @DisplayName("UTCID08 - itemType=COMBO, combo not found")
    void testCreateFromOrderComboNotFound() {
        // Arrange
        String comboId = UUID.randomUUID().toString();
        Order comboOrder = Order.builder()
                .orderId(testOrderId)
                .account(testAccount)
                .status(OrderStatus.COMPLETED)
                .itemType("COMBO")
                .itemId(comboId)
                .build();

        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(comboOrder));
        when(comboEpisodeRepository.findById(comboId)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createFromOrder(testOrderId, comboId, "COMBO", testAccountId));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).contains("Combo not found");
        verify(comboEpisodeRepository, times(1)).findById(comboId);
    }

    // ============= UTCID09: Combo is deleted (400) =============
    @Test
    @DisplayName("UTCID09 - itemType=COMBO, combo isDeleted=true")
    void testCreateFromOrderComboDeleted() {
        // Arrange
        String comboId = UUID.randomUUID().toString();
        ComboEpisode deletedCombo = newCombo(comboId, UUID.randomUUID().toString(), "Deleted Combo", null);
        deletedCombo.setIsDeleted(true);

        Order comboOrder = Order.builder()
                .orderId(testOrderId)
                .account(testAccount)
                .status(OrderStatus.COMPLETED)
                .itemType("COMBO")
                .itemId(comboId)
                .build();

        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(comboOrder));
        when(comboEpisodeRepository.findById(comboId)).thenReturn(Optional.of(deletedCombo));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createFromOrder(testOrderId, comboId, "COMBO", testAccountId));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).contains("Combo is deleted");
        verify(comboEpisodeRepository, times(1)).findById(comboId);
    }

    // ============= UTCID10: Combo has null episode list =============
    @Test
    @DisplayName("UTCID10 - itemType=COMBO, combo has null episode list")
    void testCreateFromOrderComboWithNullEpisodes() {
        // Arrange
        String comboId = UUID.randomUUID().toString();
        ComboEpisode comboWithNullEpisodes = newCombo(comboId, UUID.randomUUID().toString(), "Combo with null episodes", null);

        Order comboOrder = Order.builder()
                .orderId(testOrderId)
                .account(testAccount)
                .status(OrderStatus.COMPLETED)
                .itemType("COMBO")
                .itemId(comboId)
                .build();

        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(comboOrder));
        when(comboEpisodeRepository.findById(comboId)).thenReturn(Optional.of(comboWithNullEpisodes));

        // Act
        List<EpisodeUnlockedContent> result = service.createFromOrder(testOrderId, comboId, "COMBO", testAccountId);

        // Assert
        assertThat(result).isEmpty();
        verify(episodeUnlockedContentRepository, never()).saveAll(any(List.class));
        verify(contentAuditLogger, never()).logAction(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // ============= UTCID11: All combo episodes already unlocked (race) =============
    @Test
    @DisplayName("UTCID11 - itemType=COMBO, all episodes already unlocked (race, all duplicate)")
    void testCreateFromOrderComboAllEpisodesAlreadyUnlocked() {
        // Arrange
        String comboId = UUID.randomUUID().toString();
        Episode episode1 = newEpisode(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "Already Unlocked Episode 1", 30000L);
        Episode episode2 = newEpisode(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "Already Unlocked Episode 2", 40000L);

        ComboEpisode combo = newCombo(comboId, UUID.randomUUID().toString(), "Test Combo", List.of(episode1, episode2));

        Order comboOrder = Order.builder()
                .orderId(testOrderId)
                .account(testAccount)
                .status(OrderStatus.COMPLETED)
                .itemType("COMBO")
                .itemId(comboId)
                .build();

        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(comboOrder));
        when(comboEpisodeRepository.findById(comboId)).thenReturn(Optional.of(combo));
        // Both episodes already unlocked
        when(episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(testAccountId, episode1.getEpisodeId()))
                .thenReturn(true);
        when(episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(testAccountId, episode2.getEpisodeId()))
                .thenReturn(true);

        // Act
        List<EpisodeUnlockedContent> result = service.createFromOrder(testOrderId, comboId, "COMBO", testAccountId);

        // Assert
        assertThat(result).isEmpty();
        verify(episodeUnlockedContentRepository, never()).saveAll(any(List.class));
        verify(contentAuditLogger, never()).logAction(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // ============= UTCID12: Invalid itemType (400) =============
    @Test
    @DisplayName("UTCID12 - itemType is invalid (not EPISODE or COMBO)")
    void testCreateFromOrderInvalidItemType() {
        // Arrange
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createFromOrder(testOrderId, testEpisodeId, "INVALID_TYPE", testAccountId));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).contains("Invalid itemType");
        assertThat(exception.getReason()).contains("EPISODE or COMBO");
    }

    // ============= Additional test: UTCID13 - Combo with mixed unlocked/not unlocked episodes =============
    @Test
    @DisplayName("UTCID13 - itemType=COMBO, partial episodes already unlocked, unlock only new ones")
    void testCreateFromOrderComboPartiallyUnlocked() {
        // Arrange
        String comboId = UUID.randomUUID().toString();
        Episode unlockedEpisode = newEpisode(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "Already Unlocked Episode", 30000L);
        Episode newEpisode = newEpisode(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "New Episode to Unlock", 40000L);

        ComboEpisode combo = newCombo(comboId, UUID.randomUUID().toString(), "Test Combo", List.of(unlockedEpisode, newEpisode));

        Order comboOrder = Order.builder()
                .orderId(testOrderId)
                .account(testAccount)
                .status(OrderStatus.COMPLETED)
                .itemType("COMBO")
                .itemId(comboId)
                .build();

        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(comboOrder));
        when(comboEpisodeRepository.findById(comboId)).thenReturn(Optional.of(combo));
        // First episode already unlocked, second is new
        when(episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(testAccountId, unlockedEpisode.getEpisodeId()))
                .thenReturn(true);
        when(episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(testAccountId, newEpisode.getEpisodeId()))
                .thenReturn(false);

        EpisodeUnlockedContent newUnlockedContent = EpisodeUnlockedContent.builder()
                .id(UUID.randomUUID())
                .account(testAccount)
                .episode(newEpisode)
                .orderId(testOrderId)
                .purchasePriceVnd(40000L)
                .unlockMethod("ORDER")
                .build();

        when(episodeUnlockedContentRepository.saveAll(any(List.class)))
                .thenReturn(List.of(newUnlockedContent));

        // Act
        List<EpisodeUnlockedContent> result = service.createFromOrder(testOrderId, comboId, "COMBO", testAccountId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEpisode()).isEqualTo(newEpisode);
        assertThat(result.get(0).getOrderId()).isEqualTo(testOrderId);
        verify(episodeUnlockedContentRepository, times(1)).saveAll(any(List.class));
        verify(contentAuditLogger, times(1)).logAction(anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
