package com.panorama.backend.mapper;

import com.panorama.backend.DTO.PoiDTO;
import com.panorama.backend.model.resource.POI;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author: DMK
 * @description:
 * @date: 2024-12-18 14:54:11
 * @version: 1.0
 */
@Mapper
public interface POIMapper {
    List<POI> getAllPOI();
    void addPOI(PoiDTO poiDTO);
    void deletePOI(int id);
}
