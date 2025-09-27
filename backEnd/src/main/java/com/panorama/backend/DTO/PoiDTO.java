package com.panorama.backend.DTO;

import lombok.Data;

/**
 * @author: DMK
 * @description:
 * @date: 2024-12-18 15:25:32
 * @version: 1.0
 */
@Data
public class PoiDTO {
    private String name;
    private String description;
    private double lat;
    private double lon;
    private double zoom;
}
