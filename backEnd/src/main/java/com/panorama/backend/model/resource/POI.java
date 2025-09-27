package com.panorama.backend.model.resource;

import lombok.Builder;
import lombok.Data;

/**
 * @author: DMK
 * @description:
 * @date: 2024-12-18 15:01:39
 * @version: 1.0
 */
@Data
@Builder
public class POI {
    private int id;
    private String name;
    private String description;
    private double lat;
    private double lon;
    private double zoom;
}
