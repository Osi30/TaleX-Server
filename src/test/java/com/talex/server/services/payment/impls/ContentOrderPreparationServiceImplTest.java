package com.talex.server.services.payment.impls;

import com.talex.server.entities.series.ComboEpisode;
import com.talex.server.entities.series.Episode;
import com.talex.server.enums.series.EpisodeStatus;
import com.talex.server.exceptions.codes.payment.PaymentErrorCode;
import com.talex.server.exceptions.details.payment.PaymentException;
import com.talex.server.exceptions.details.ResourceNotFoundException;
import com.talex.server.repositories.series.ComboEpisodeRepository;
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.repositories.series.EpisodeUnlockedContentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentOrderPreparationService Tests")
class ContentOrderPreparationServiceImplTest {

    @Mock
    private EpisodeRepository episodeRepository;

    @Mock
    private ComboEpisodeRepository comboEpisodeRepository;

    @Mock
    private EpisodeUnlockedContentRepository episodeUnlockedContentRepository;

    @InjectMocks
    private ContentOrderPreparationServiceImpl service;

    private UUID testAccountId;
    private String testEpisodeId;
    private String testComboId;

    @BeforeEach
    void setUp() {
        testAccountId = UUID.randomUUID();
        testEpisodeId = UUID.randomUUID().toString();
        testComboId = UUID.randomUUID().toString();
    }

    // ==================== EPISODE TESTS ====================

    @Test
    @DisplayName("UTCID01: resolvePrice() với Episode chưa sở hữu → trả giá gốc")
    void testResolveEpisodePriceNotOwnedReturnsPrice() {
        // Arrange
        Episode episode = new Episode();
        episode.setEpisodeId(testEpisodeId);
        episode.setStatus(EpisodeStatus.PUBLISHED);
        episode.setPriceVnd(5000L);

        when(episodeRepository.findById(testEpisodeId)).thenReturn(Optional.of(episode));
        when(episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(testAccountId, testEpisodeId))
                .thenReturn(false);

        // Act
        ContentOrderPreparationServiceImpl.ContentPriceResolution result = service.resolvePrice(testAccountId, "EPISODE", testEpisodeId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.payablePrice()).isEqualTo(BigDecimal.valueOf(5000L));
        assertThat(result.originalPrice()).isEqualTo(BigDecimal.valueOf(5000L));
        assertThat(result.ownedEpisodeCount()).isEqualTo(0);
        assertThat(result.totalEpisodeCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("UTCID02: resolvePrice() với Episode đã sở hữu → throw CONTENT_ALREADY_OWNED")
    void testResolveEpisodePriceAlreadyOwnedThrows() {
        // Arrange
        Episode episode = new Episode();
        episode.setEpisodeId(testEpisodeId);
        episode.setStatus(EpisodeStatus.PUBLISHED);
        episode.setPriceVnd(5000L);

        when(episodeRepository.findById(testEpisodeId)).thenReturn(Optional.of(episode));
        when(episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(testAccountId, testEpisodeId))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.resolvePrice(testAccountId, "EPISODE", testEpisodeId))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode").isEqualTo(PaymentErrorCode.CONTENT_ALREADY_OWNED);
    }

    @Test
    @DisplayName("UTCID03: resolvePrice() với Episode không tồn tại → throw ResourceNotFoundException")
    void testResolveEpisodePriceNotFoundThrows() {
        // Arrange
        when(episodeRepository.findById(testEpisodeId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.resolvePrice(testAccountId, "EPISODE", testEpisodeId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Episode not found");
    }

    @Test
    @DisplayName("UTCID04: resolvePrice() với Episode unpublished → throw ResourceNotFoundException")
    void testResolveEpisodePriceUnpublishedThrows() {
        // Arrange
        Episode episode = new Episode();
        episode.setEpisodeId(testEpisodeId);
        episode.setStatus(EpisodeStatus.DRAFT);
        episode.setPriceVnd(5000L);

        when(episodeRepository.findById(testEpisodeId)).thenReturn(Optional.of(episode));

        // Act & Assert
        assertThatThrownBy(() -> service.resolvePrice(testAccountId, "EPISODE", testEpisodeId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Episode not found");
    }

    // ==================== COMBO TESTS ====================

    @Test
    @DisplayName("UTCID05: resolvePrice() với Combo toàn bộ tập đã sở hữu → throw CONTENT_ALREADY_OWNED")
    void testResolveComboPrice_AllOwnedThrows() {
        // Arrange
        Episode ep1 = new Episode();
        ep1.setEpisodeId(UUID.randomUUID().toString());
        Episode ep2 = new Episode();
        ep2.setEpisodeId(UUID.randomUUID().toString());
        Episode ep3 = new Episode();
        ep3.setEpisodeId(UUID.randomUUID().toString());

        ComboEpisode combo = new ComboEpisode();
        combo.setComboId(testComboId);
        combo.setIsDeleted(false);
        combo.setPriceVnd(12000L);
        combo.setEpisodes(List.of(ep1, ep2, ep3));

        when(comboEpisodeRepository.findById(testComboId)).thenReturn(Optional.of(combo));
        when(episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(
                eq(testAccountId), anyString()))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.resolvePrice(testAccountId, "COMBO", testComboId))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode").isEqualTo(PaymentErrorCode.CONTENT_ALREADY_OWNED);
    }

    @Test
    @DisplayName("UTCID06: resolvePrice() với Combo không sở hữu tập nào → trả giá gốc combo")
    void testResolveComboPrice_NoOwnedReturnsComboPrice() {
        // Arrange
        Episode ep1 = new Episode();
        ep1.setEpisodeId(UUID.randomUUID().toString());
        Episode ep2 = new Episode();
        ep2.setEpisodeId(UUID.randomUUID().toString());
        Episode ep3 = new Episode();
        ep3.setEpisodeId(UUID.randomUUID().toString());

        ComboEpisode combo = new ComboEpisode();
        combo.setComboId(testComboId);
        combo.setIsDeleted(false);
        combo.setPriceVnd(12000L);
        combo.setEpisodes(List.of(ep1, ep2, ep3));

        when(comboEpisodeRepository.findById(testComboId)).thenReturn(Optional.of(combo));
        when(episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(
                eq(testAccountId), anyString()))
                .thenReturn(false);

        // Act
        ContentOrderPreparationServiceImpl.ContentPriceResolution result = service.resolvePrice(testAccountId, "COMBO", testComboId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.payablePrice()).isEqualTo(BigDecimal.valueOf(12000L));
        assertThat(result.originalPrice()).isEqualTo(BigDecimal.valueOf(12000L));
        assertThat(result.ownedEpisodeCount()).isEqualTo(0);
        assertThat(result.totalEpisodeCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("UTCID07: resolvePrice() với Combo không tồn tại → throw ResourceNotFoundException")
    void testResolveComboPrice_NotFoundThrows() {
        // Arrange
        when(comboEpisodeRepository.findById(testComboId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.resolvePrice(testAccountId, "COMBO", testComboId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Combo not found");
    }

    @Test
    @DisplayName("UTCID08: resolvePrice() với Combo deleted → throw ResourceNotFoundException")
    void testResolveComboPrice_DeletedThrows() {
        // Arrange
        ComboEpisode combo = new ComboEpisode();
        combo.setComboId(testComboId);
        combo.setIsDeleted(true);
        combo.setPriceVnd(12000L);

        when(comboEpisodeRepository.findById(testComboId)).thenReturn(Optional.of(combo));

        // Act & Assert
        assertThatThrownBy(() -> service.resolvePrice(testAccountId, "COMBO", testComboId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Combo not found");
    }

    @Test
    @DisplayName("UTCID09: resolvePrice() Combo sở hữu 1/3 tập → trừ đúng giá quy đổi (FLOOR)")
    void testResolveComboPrice_PartialOwned_OneThirdReturnsDiscountedPrice() {
        // Arrange: Combo 12000 VND, 3 tập. User sở hữu 1 tập
        // perEpisodeRate = 12000 / 3 = 4000 (FLOOR)
        // discount = 4000 * 1 = 4000
        // payable = 12000 - 4000 = 8000
        String ownedEpisodeId = UUID.randomUUID().toString();
        Episode ep1 = new Episode();
        ep1.setEpisodeId(ownedEpisodeId);
        Episode ep2 = new Episode();
        ep2.setEpisodeId(UUID.randomUUID().toString());
        Episode ep3 = new Episode();
        ep3.setEpisodeId(UUID.randomUUID().toString());

        ComboEpisode combo = new ComboEpisode();
        combo.setComboId(testComboId);
        combo.setIsDeleted(false);
        combo.setPriceVnd(12000L);
        combo.setEpisodes(List.of(ep1, ep2, ep3));

        when(comboEpisodeRepository.findById(testComboId)).thenReturn(Optional.of(combo));
        // Fallback (khớp mọi episodeId) PHẢI đăng ký TRƯỚC — Mockito ưu tiên stub đăng ký
        // SAU CÙNG khi nhiều stub cùng khớp 1 lời gọi, nên override cụ thể phải nằm sau.
        when(episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(eq(testAccountId), anyString()))
                .thenReturn(false);
        // Only ownedEpisodeId returns true
        when(episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(testAccountId, ownedEpisodeId))
                .thenReturn(true);

        // Act
        ContentOrderPreparationServiceImpl.ContentPriceResolution result = service.resolvePrice(testAccountId, "COMBO", testComboId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.payablePrice()).isEqualTo(BigDecimal.valueOf(8000L));
        assertThat(result.originalPrice()).isEqualTo(BigDecimal.valueOf(12000L));
        assertThat(result.ownedEpisodeCount()).isEqualTo(1);
        assertThat(result.totalEpisodeCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("UTCID10: resolvePrice() Combo sở hữu 2/3 tập → trừ đúng giá quy đổi (FLOOR)")
    void testResolveComboPrice_PartialOwned_TwoThirdsReturnsDiscountedPrice() {
        // Arrange: Combo 12000 VND, 3 tập. User sở hữu 2 tập
        // perEpisodeRate = 12000 / 3 = 4000 (FLOOR)
        // discount = 4000 * 2 = 8000
        // payable = 12000 - 8000 = 4000
        String ownedEp1Id = UUID.randomUUID().toString();
        String ownedEp2Id = UUID.randomUUID().toString();
        Episode ep1 = new Episode();
        ep1.setEpisodeId(ownedEp1Id);
        Episode ep2 = new Episode();
        ep2.setEpisodeId(ownedEp2Id);
        Episode ep3 = new Episode();
        ep3.setEpisodeId(UUID.randomUUID().toString());

        ComboEpisode combo = new ComboEpisode();
        combo.setComboId(testComboId);
        combo.setIsDeleted(false);
        combo.setPriceVnd(12000L);
        combo.setEpisodes(List.of(ep1, ep2, ep3));

        when(comboEpisodeRepository.findById(testComboId)).thenReturn(Optional.of(combo));
        // Fallback đăng ký TRƯỚC — xem giải thích ở test UTCID09.
        when(episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(eq(testAccountId), anyString()))
                .thenReturn(false);
        when(episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(testAccountId, ownedEp1Id))
                .thenReturn(true);
        when(episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(testAccountId, ownedEp2Id))
                .thenReturn(true);

        // Act
        ContentOrderPreparationServiceImpl.ContentPriceResolution result = service.resolvePrice(testAccountId, "COMBO", testComboId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.payablePrice()).isEqualTo(BigDecimal.valueOf(4000L));
        assertThat(result.originalPrice()).isEqualTo(BigDecimal.valueOf(12000L));
        assertThat(result.ownedEpisodeCount()).isEqualTo(2);
        assertThat(result.totalEpisodeCount()).isEqualTo(3);
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("UTCID_EDGE01: Combo rỗng (0 tập) → trả giá gốc")
    void testResolveComboPrice_EmptyComboReturnsOriginalPrice() {
        // Arrange
        ComboEpisode combo = new ComboEpisode();
        combo.setComboId(testComboId);
        combo.setIsDeleted(false);
        combo.setPriceVnd(5000L);
        combo.setEpisodes(new ArrayList<>()); // empty

        when(comboEpisodeRepository.findById(testComboId)).thenReturn(Optional.of(combo));

        // Act
        ContentOrderPreparationServiceImpl.ContentPriceResolution result = service.resolvePrice(testAccountId, "COMBO", testComboId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.payablePrice()).isEqualTo(BigDecimal.valueOf(5000L));
        assertThat(result.originalPrice()).isEqualTo(BigDecimal.valueOf(5000L));
        assertThat(result.ownedEpisodeCount()).isEqualTo(0);
        assertThat(result.totalEpisodeCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("UTCID_EDGE02: Combo giá 11000, 3 tập → perEpisodeRate = 11000/3 FLOOR = 3666")
    void testResolveComboPrice_FloorRoundingBehavior() {
        // Arrange: Combo 11000 VND, 3 tập, 1 tập đã sở hữu
        // perEpisodeRate = 11000 / 3 = 3666.666... FLOOR = 3666
        // discount = 3666 * 1 = 3666
        // payable = 11000 - 3666 = 7334
        String ownedEpisodeId = UUID.randomUUID().toString();
        Episode ep1 = new Episode();
        ep1.setEpisodeId(ownedEpisodeId);
        Episode ep2 = new Episode();
        ep2.setEpisodeId(UUID.randomUUID().toString());
        Episode ep3 = new Episode();
        ep3.setEpisodeId(UUID.randomUUID().toString());

        ComboEpisode combo = new ComboEpisode();
        combo.setComboId(testComboId);
        combo.setIsDeleted(false);
        combo.setPriceVnd(11000L);
        combo.setEpisodes(List.of(ep1, ep2, ep3));

        when(comboEpisodeRepository.findById(testComboId)).thenReturn(Optional.of(combo));
        // Fallback đăng ký TRƯỚC — xem giải thích ở test UTCID09.
        when(episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(eq(testAccountId), anyString()))
                .thenReturn(false);
        when(episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(testAccountId, ownedEpisodeId))
                .thenReturn(true);

        // Act
        ContentOrderPreparationServiceImpl.ContentPriceResolution result = service.resolvePrice(testAccountId, "COMBO", testComboId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.payablePrice()).isEqualTo(BigDecimal.valueOf(7334L));
        assertThat(result.originalPrice()).isEqualTo(BigDecimal.valueOf(11000L));
    }

    @Test
    @DisplayName("normalizeItemType() EPISODE → trả EPISODE")
    void testNormalizeItemTypeEpisode() {
        // Act
        String normalized = service.normalizeItemType("episode");

        // Assert
        assertThat(normalized).isEqualTo("EPISODE");
    }

    @Test
    @DisplayName("normalizeItemType() COMBO → trả COMBO")
    void testNormalizeItemTypeCombo() {
        // Act
        String normalized = service.normalizeItemType("combo");

        // Assert
        assertThat(normalized).isEqualTo("COMBO");
    }

    @Test
    @DisplayName("normalizeItemType() invalid type → throw INVALID_ITEM_TYPE")
    void testNormalizeItemTypeInvalid() {
        // Act & Assert
        assertThatThrownBy(() -> service.normalizeItemType("INVALID"))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode").isEqualTo(PaymentErrorCode.INVALID_ITEM_TYPE);
    }
}
