package com.panorama.backend.DTO.admin;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LayerListResponse {
    private List<LayerSummaryDTO> items;
    private int page;
    private int size;
    private long total;
}
