package com.panorama.backend.DTO;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 地理概览分析结果 DTO
 *
 * 用于承载行政区划、边界面积、人口密度、主要城市与关键基础设施等基础数据。
 * 目前结构与前端地理分析界面的展示结构保持一致，后续可根据真实业务数据源进行细化。
 */
@Data
@Builder
public class GeoOverviewDTO {

    /**
     * 行政区划信息列表，例如：省级、地市级、重点边境县等
     * 每个元素建议包含字段：
     * - name: 名称
     * - count: 数量
     * - note: 备注
     */
    private List<Map<String, Object>> adminDivisions;

    /**
     * 边界与面积信息：
     * - area: 国土面积
     * - coastline: 海岸线长度
     * - neighbors: 接壤国家列表
     */
    private Map<String, Object> boundary;

    /**
     * 人口密度信息：
     * - average: 平均人口密度
     * - highest: { region, density }
     * - lowest: { region, density }
     */
    private Map<String, Object> populationDensity;

    /**
     * 主要城市信息列表：
     * - name: 城市名称
     * - population: 人口规模（可选）
     * - role: 功能定位（政治中心/港口枢纽/科技产业等）
     * - lat/lon/zoom: 地图视角（可选）
     */
    private List<Map<String, Object>> majorCities;

    /**
     * 关键基础设施：
     * - energy: 能源设施列表
     * - transport: 交通枢纽列表（机场、港口、高铁等）
     * - communication: 通信设施列表（地面站、光缆等）
     */
    private Map<String, List<String>> infrastructures;
}


