package com.panorama.backend.DTO.admin;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class LayerSummaryDTO {
    private String id;
    private String layerName;
    private String tableName;
    private String category;
    private Map<String, String> usage;
    private Long createdAt;
    private Long updatedAt;
}
