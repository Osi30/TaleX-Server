package com.talex.server.services.recommend.impls;

import com.talex.server.dtos.recommend.request.SeriesChannelConfigReq;
import com.talex.server.dtos.recommend.response.SeriesChannelConfigRes;
import com.talex.server.entities.config.SeriesChannelConfig;
import com.talex.server.exceptions.details.ResourceNotFoundException;
import com.talex.server.repositories.trending.SeriesChannelConfigRepository;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeriesChannelConfigServiceImpl Tests")
class SeriesChannelConfigServiceImplTest {

    @Mock
    private SeriesChannelConfigRepository configRepository;

    @InjectMocks
    private SeriesChannelConfigServiceImpl configService;

    private SeriesChannelConfig sampleConfig;
    private SeriesChannelConfigReq sampleReq;

    @BeforeEach
    void setUp() {
        sampleConfig = SeriesChannelConfig.builder()
                .configId("1")
                .trendingPoolNumber(10)
                .promotedPoolNumber(5)
                .newReleasedPoolNumber(15)
                .latestCommunityChoicePoolNumber(8)
                .communityChoicePoolNumber(12)
                .recentlyUpdatedPoolNumber(20)
                .randomCategoryPoolNumber(30)
                .subscribedPoolNumber(25)
                .numberPerCategory(3)
                .updatedAt(LocalDateTime.now())
                .build();

        sampleReq = SeriesChannelConfigReq.builder()
                .trendingPoolNumber(10)
                .promotedPoolNumber(5)
                .newReleasedPoolNumber(15)
                .latestCommunityChoicePoolNumber(8)
                .communityChoicePoolNumber(12)
                .recentlyUpdatedPoolNumber(20)
                .randomCategoryPoolNumber(30)
                .subscribedPoolNumber(25)
                .numberPerCategory(3)
                .build();
    }

    @Test
    @DisplayName("getConfig - Success")
    void getConfig_Success() {
        when(configRepository.findFirstBy()).thenReturn(Optional.of(sampleConfig));

        SeriesChannelConfigRes res = configService.getConfig();

        assertThat(res).isNotNull();
        assertThat(res.getTrendingPoolNumber()).isEqualTo(10);
        assertThat(res.getPromotedPoolNumber()).isEqualTo(5);
        verify(configRepository, times(1)).findFirstBy();
    }

    @Test
    @DisplayName("getConfig - ResourceNotFoundException when config absent")
    void getConfig_NotFound() {
        when(configRepository.findFirstBy()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configService.getConfig())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cấu hình SeriesChannelConfig chưa được khởi tạo");
    }

    @Test
    @DisplayName("createConfig - Success when count is 0")
    void createConfig_Success() throws BadRequestException {
        when(configRepository.count()).thenReturn(0L);
        when(configRepository.save(any(SeriesChannelConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SeriesChannelConfigRes res = configService.createConfig(sampleReq);

        assertThat(res).isNotNull();
        assertThat(res.getTrendingPoolNumber()).isEqualTo(10);
        verify(configRepository, times(1)).save(any(SeriesChannelConfig.class));
    }

    @Test
    @DisplayName("createConfig - Throws BadRequestException when count > 0")
    void createConfig_AlreadyExists() {
        when(configRepository.count()).thenReturn(1L);

        assertThatThrownBy(() -> configService.createConfig(sampleReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cấu hình SeriesChannelConfig đã được khởi tạo trước đó");

        verify(configRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateConfig - Success")
    void updateConfig_Success() throws BadRequestException {
        when(configRepository.findFirstBy()).thenReturn(Optional.of(sampleConfig));
        when(configRepository.save(any(SeriesChannelConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SeriesChannelConfigReq updateReq = SeriesChannelConfigReq.builder()
                .trendingPoolNumber(50)
                .promotedPoolNumber(20)
                .newReleasedPoolNumber(30)
                .latestCommunityChoicePoolNumber(15)
                .communityChoicePoolNumber(25)
                .recentlyUpdatedPoolNumber(40)
                .randomCategoryPoolNumber(60)
                .subscribedPoolNumber(35)
                .numberPerCategory(5)
                .build();

        SeriesChannelConfigRes res = configService.updateConfig(updateReq);

        assertThat(res).isNotNull();
        assertThat(res.getTrendingPoolNumber()).isEqualTo(50);
        assertThat(res.getPromotedPoolNumber()).isEqualTo(20);
        verify(configRepository, times(1)).save(sampleConfig);
    }

    @Test
    @DisplayName("updateConfig - Throws ResourceNotFoundException when not existing")
    void updateConfig_NotFound() {
        when(configRepository.findFirstBy()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configService.updateConfig(sampleReq))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Chưa có cấu hình SeriesChannelConfig trong hệ thống để cập nhật");

        verify(configRepository, never()).save(any());
    }
}
