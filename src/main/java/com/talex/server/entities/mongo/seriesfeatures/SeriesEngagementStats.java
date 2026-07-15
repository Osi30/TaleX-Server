package com.talex.server.entities.mongo.seriesfeatures;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeriesEngagementStats {
    @Field("total_watch_time") @Builder.Default private Double totalWatchTime = 0D;
    @Field("watch_time_last_7d") @Builder.Default private Double watchTimeLast7d = 0D;
    @Field("watch_time_last_24h") @Builder.Default private Double watchTimeLast24h = 0D;
}
