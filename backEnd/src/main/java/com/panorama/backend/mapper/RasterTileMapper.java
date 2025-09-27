package com.panorama.backend.mapper;

import com.panorama.backend.model.resource.Tile;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author: DMK
 * @description:
 * @date: 2024-10-16 10:30:50
 * @version: 1.0
 */
@Mapper
public interface RasterTileMapper {
    Tile getRasterDBTile(int z, int x, int y);
}
