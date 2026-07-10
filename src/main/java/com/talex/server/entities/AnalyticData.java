package com.talex.server.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticData {
    @Column(nullable = false, columnDefinition = "bigint default 0")
    @Builder.Default
    private Long likes = 0L;

    @Column(nullable = false, columnDefinition = "bigint default 0")
    @Builder.Default
    private Long views = 0L;

    @Column(nullable = false, columnDefinition = "bigint default 0")
    @Builder.Default
    private Long comments = 0L;

    @Column(nullable = false, columnDefinition = "bigint default 0")
    @Builder.Default
    private Long shares = 0L;

    @Column(nullable = false, columnDefinition = "bigint default 0")
    @Builder.Default
    private Long bookmarks = 0L;

    @Column(name = "watch_time", nullable = false, columnDefinition = "float8 default 0.0")
    @Builder.Default
    private Double watchTime = 0D;
}
