package com.talex.server.entities.media;

import com.talex.server.entities.BaseAudit;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "media_system_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaSystemConfig extends BaseAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "config_id", updatable = false, nullable = false)
    private UUID configId;

    @Column(name = "max_comic_images", nullable = false)
    @Builder.Default
    private Integer maxComicImages = 20;

    @Column(name = "max_comic_image_size_mb", nullable = false)
    @Builder.Default
    private Double maxComicImageSizeMb = 3.0;

    @Column(name = "max_video_size_mb", nullable = false)
    @Builder.Default
    private Double maxVideoSizeMb = 30.0;
}
