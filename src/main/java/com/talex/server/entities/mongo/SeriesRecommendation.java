package com.talex.server.entities.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "series_recommendations")
public class SeriesRecommendation {
    
    @Id
    private String id; // seriesId
    
    private List<String> similarIds;
    
    private Instant updatedAt;
}
