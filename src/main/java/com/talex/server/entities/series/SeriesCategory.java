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
        name = "series_categories",
        indexes = {
                @Index(name = "idx_series_categories_series_deleted", columnList = "series_id,is_deleted"),
                @Index(name = "idx_series_categories_category_deleted", columnList = "category_id,is_deleted")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeriesCategory extends BaseTimeEntity {
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
