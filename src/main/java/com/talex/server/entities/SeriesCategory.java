package com.talex.server.entities;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "series_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeriesCategory extends BaseAudit {
    @EmbeddedId
    private SeriesCategoryId id = new SeriesCategoryId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("seriesId")
    @JoinColumn(name = "series_id", nullable = false)
    private Series series;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("categoryId")
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    public SeriesCategory(Series series, Category category) {
        this.series = series;
        this.category = category;
        this.id = new SeriesCategoryId(series.getSeriesId(), category.getCategoryId());
    }
}
