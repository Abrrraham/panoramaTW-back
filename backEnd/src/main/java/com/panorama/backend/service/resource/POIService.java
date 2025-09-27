package com.panorama.backend.service.resource;

import com.panorama.backend.DTO.PoiDTO;
import com.panorama.backend.mapper.POIMapper;
import com.panorama.backend.model.Constant.GenerateResultStatus;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.model.resource.POI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author: DMK
 * @description:
 * @date: 2024-12-18 15:10:17
 * @version: 1.0
 */
@Service
@Slf4j
public class POIService {
    private final POIMapper poiMapper;

    @Autowired
    public POIService(POIMapper poiMapper) {
        this.poiMapper = poiMapper;
    }

    public List<POI> getAllPOI() {
        return poiMapper.getAllPOI();
    }

    public GeneralResult addPOI(PoiDTO poiDTO) {
        try {
            poiMapper.addPOI(poiDTO);
            return GeneralResult.builder().status(GenerateResultStatus.SUCCESS).message("add poi successfully").build();
        } catch (Exception e) {
            return GeneralResult.builder().status(GenerateResultStatus.ERROR).message("failed to add poi").build();
        }
    }

    public GeneralResult deletePOI(int id) {
        try {
            poiMapper.deletePOI(id);
            return GeneralResult.builder().status(GenerateResultStatus.SUCCESS).message("delete poi successfully").build();
        } catch (Exception e) {
            return GeneralResult.builder().status(GenerateResultStatus.ERROR).message("failed to delete poi").build();
        }
    }
}
