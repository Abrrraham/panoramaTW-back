package com.panorama.backend.DTO.admin;

import lombok.Data;

@Data
public class LayerUpdateRequest {
    private String layerName;
    private String status;
}
