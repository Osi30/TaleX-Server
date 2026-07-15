package com.talex.server.entities.mongo.userfeatures;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeepEngagementStats {
    @Field("total_watch_time") private Double totalWatchTime = 0D;
    @Field("watch_time_last_24h") private Double watchTimeLast24h = 0D;
    @Field("watch_time_last_7d") private Double watchTimeLast7d = 0D;
}
