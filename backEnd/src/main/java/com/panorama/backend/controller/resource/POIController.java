package com.panorama.backend.controller.resource;

import com.panorama.backend.DTO.PoiDTO;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.model.resource.POI;
import com.panorama.backend.service.resource.POIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author: DMK
 * @description:
 * @date: 2024-12-18 15:30:31
 * @version: 1.0
 */
@RestController
@RequestMapping("api/v0/resource/poi")
public class POIController {
    private final POIService poiService;

    @Autowired
    public POIController(POIService poiService) {
        this.poiService = poiService;
    }

    @GetMapping("all")
    public ResponseEntity<List<POI>> getAllPOI() {
        return ResponseEntity.ok(poiService.getAllPOI());
    }

    @PostMapping("add")
    public ResponseEntity<GeneralResult> addPOI(@RequestBody PoiDTO poiDTO) {
        return ResponseEntity.ok(poiService.addPOI(poiDTO));
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<GeneralResult> deletePOI(@PathVariable int id) {
        return ResponseEntity.ok(poiService.deletePOI(id));
    }
}
