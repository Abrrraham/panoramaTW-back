package com.panorama.backend.model.resource;

import lombok.Data;

/**
 * @author: DMK
 * @description:
 * @date: 2025-03-03 20:42:02
 * @version: 1.0
 */
@Data
public class Tile {
    int id;
    int zoom_level;
    int tile_column;
    int tile_row;
    byte[] tile_data;
}
