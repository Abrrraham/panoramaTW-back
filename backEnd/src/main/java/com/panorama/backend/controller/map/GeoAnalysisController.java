package com.panorama.backend.controller.map;

import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.service.map.GeoAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 地理分析相关接口
 *
 * 当前提供：
 * - /api/v0/map/analysis/overview ：地理概览分析
 */
@RestController
@RequestMapping("api/v0/map/analysis")
public class GeoAnalysisController {

    private final GeoAnalysisService geoAnalysisService;

    @Autowired
    public GeoAnalysisController(GeoAnalysisService geoAnalysisService) {
        this.geoAnalysisService = geoAnalysisService;
    }

    /**
     * 地理概览分析接口
     *
     * 返回行政区划、边界与面积、人口密度、主要城市和关键基础设施等概要信息，
     * 以便前端地理分析页面调用并叠加到地图或面板中展示。
     */
    @GetMapping("overview")
    public ResponseEntity<GeneralResult> getGeoOverview() {
        GeneralResult result = geoAnalysisService.getGeoOverview();
        return ResponseEntity.ok(result);
    }
}


