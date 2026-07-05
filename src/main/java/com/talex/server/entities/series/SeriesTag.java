package com.talex.server.entities.series;

import com.talex.server.entities.BaseTimeEntity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "series_tags",
        indexes = {
                @Index(name = "idx_series_tags_series_deleted", columnList = "series_id,is_deleted"),
                @Index(name = "idx_series_tags_tag_deleted", columnList = "tag_id,is_deleted")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeriesTag extends BaseTimeEntity {
    @EmbeddedId
    private SeriesTagId id = new SeriesTagId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("seriesId")
    @JoinColumn(name = "series_id", nullable = false)
    private Series series;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("tagId")
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public SeriesTag(Series series, Tag tag) {
        this.series = series;
        this.tag = tag;
        this.id = new SeriesTagId(series.getSeriesId(), tag.getTagId());
    }
}
