package com.talex.server.entities.series;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeriesTagId implements Serializable {
    @Column(name = "series_id")
    private String seriesId;

    @Column(name = "tag_id")
    private String tagId;
}
