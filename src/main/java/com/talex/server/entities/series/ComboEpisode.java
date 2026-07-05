package com.talex.server.entities.series;

import com.talex.server.entities.BaseTimeEntity;
import com.talex.server.enums.series.EpisodeStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "combo_episodes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComboEpisode extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "combo_id")
    private String comboId;

    @Column(name = "creator_id", nullable = false)
    private String creatorId;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EpisodeStatus status = EpisodeStatus.DRAFT;

    @Column(name = "price_vnd", nullable = false, columnDefinition = "bigint default 0")
    private Long priceVnd = 0L;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "combo_episode_mapping",
            joinColumns = @JoinColumn(name = "combo_id"),
            inverseJoinColumns = @JoinColumn(name = "episode_id")
    )
    private List<Episode> episodes = new ArrayList<>();
}
