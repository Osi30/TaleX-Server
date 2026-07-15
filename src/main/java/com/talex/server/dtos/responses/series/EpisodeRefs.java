package com.talex.server.dtos.responses.series;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeRefs implements Serializable {
    private static final long serialVersionUID = 1L;

    private String episodeId;
    private List<String> tags;
    private List<String> categories;
}