package com.panorama.backend.service.map;

import com.panorama.backend.DTO.GeoOverviewDTO;
import com.panorama.backend.model.Constant.GenerateResultStatus;
import com.panorama.backend.model.node.LayerNode;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.model.resource.POI;
import com.panorama.backend.repository.LayerNodeRepo;
import com.panorama.backend.service.resource.POIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 地理分析相关算法服务
 *
 * 说明：
 * - 目前示例实现了“地理概览分析”功能，主要依赖已有的 POI 表与 LayerNode 信息；
 * - 行政区划、面积、人口密度等精确指标通常需要专门的统计或矢量数据表，
 *   这里先给出占位结构及简单示例，后续可按真实数据源完善计算逻辑。
 */
@Service
@Slf4j
public class GeoAnalysisService {

    private final POIService poiService;
    private final LayerNodeRepo layerNodeRepo;

    @Autowired
    public GeoAnalysisService(POIService poiService, LayerNodeRepo layerNodeRepo) {
        this.poiService = poiService;
        this.layerNodeRepo = layerNodeRepo;
    }

    /**
     * 地理概览分析：
     * - 行政区划统计：基于 LayerNode 分类做简单计数（示例）
     * - 主要城市 & 港口：基于 POI 表中名称/描述关键字做筛选
     * - 其他指标先给出示例结构，方便前端接入展示
     */
    public GeneralResult getGeoOverview() {
        try {
            List<POI> pois = poiService.getAllPOI();

            // 1. 基于 POI 粗略提取“主要城市”和“港口”示例
            List<Map<String, Object>> majorCities = pois.stream()
                    .filter(poi -> isCityPOI(poi))
                    .limit(10)
                    .map(this::toCityInfo)
                    .collect(Collectors.toList());

            List<String> ports = pois.stream()
                    .filter(this::isPortPOI)
                    .map(POI::getName)
                    .distinct()
                    .limit(20)
                    .collect(Collectors.toList());

            // 2. 行政区划示例统计（实际应来自行政区划表或图层）
            List<Map<String, Object>> adminDivisions = buildAdminDivisionOverview();

            // 3. 边界与面积 / 人口密度 示例（占位，可按实际业务替换）
            Map<String, Object> boundary = buildBoundaryOverview();
            Map<String, Object> populationDensity = buildPopulationDensityOverview();

            // 4. 关键基础设施示例：基于 POI 关键字简单分类
            Map<String, List<String>> infrastructures = buildInfrastructureOverview(pois, ports);

            GeoOverviewDTO overviewDTO = GeoOverviewDTO.builder()
                    .adminDivisions(adminDivisions)
                    .boundary(boundary)
                    .populationDensity(populationDensity)
                    .majorCities(majorCities)
                    .infrastructures(infrastructures)
                    .build();

            return GeneralResult.builder()
                    .status(GenerateResultStatus.SUCCESS)
                    .message("geo overview success")
                    .data(overviewDTO)
                    .build();
        } catch (Exception e) {
            log.error("Failed to generate geo overview", e);
            return GeneralResult.builder()
                    .status(GenerateResultStatus.ERROR)
                    .message("failed to generate geo overview: " + e.getMessage())
                    .build();
        }
    }

    private boolean isCityPOI(POI poi) {
        String name = Optional.ofNullable(poi.getName()).orElse("").toLowerCase();
        String desc = Optional.ofNullable(poi.getDescription()).orElse("").toLowerCase();
        // 依据名称或描述中的关键字简单判断城市类 POI，实际应使用专门字段
        return name.contains("市") || name.contains("城") || desc.contains("city") || desc.contains("capital");
    }

    private boolean isPortPOI(POI poi) {
        String name = Optional.ofNullable(poi.getName()).orElse("").toLowerCase();
        String desc = Optional.ofNullable(poi.getDescription()).orElse("").toLowerCase();
        return name.contains("港") || desc.contains("港") || desc.contains("port");
    }

    private Map<String, Object> toCityInfo(POI poi) {
        Map<String, Object> city = new HashMap<>();
        city.put("name", poi.getName());
        city.put("population", null); // 需接入人口统计表后填充
        city.put("role", Optional.ofNullable(poi.getDescription()).orElse("城市/重要节点"));
        city.put("lat", poi.getLat());
        city.put("lon", poi.getLon());
        city.put("zoom", poi.getZoom());
        return city;
    }

    private List<Map<String, Object>> buildAdminDivisionOverview() {
        List<Map<String, Object>> list = new ArrayList<>();

        // 示例：基于 LayerNode 的简单计数，真实场景应使用行政区划表
        try {
            List<LayerNode> vectorLayers = layerNodeRepo.findLayerNodesByCategory("vector");
            Map<String, Long> byCategory = vectorLayers.stream()
                    .collect(Collectors.groupingBy(LayerNode::getCategory, Collectors.counting()));

            Map<String, Object> province = new HashMap<>();
            province.put("name", "省级行政单元");
            province.put("count", byCategory.getOrDefault("province", 0L));
            province.put("note", "示例统计，需接入真实行政区划数据");
            list.add(province);

            Map<String, Object> city = new HashMap<>();
            city.put("name", "地市级行政单元");
            city.put("count", byCategory.getOrDefault("city", 0L));
            city.put("note", "示例统计，需接入真实行政区划数据");
            list.add(city);
        } catch (Exception e) {
            log.warn("buildAdminDivisionOverview failed, use placeholder", e);
        }

        return list;
    }

    private Map<String, Object> buildBoundaryOverview() {
        Map<String, Object> boundary = new HashMap<>();
        boundary.put("area", null); // 示例，占位
        boundary.put("coastline", null); // 示例，占位
        boundary.put("neighbors", Collections.emptyList());
        return boundary;
    }

    private Map<String, Object> buildPopulationDensityOverview() {
        Map<String, Object> pd = new HashMap<>();
        pd.put("average", null);

        Map<String, Object> highest = new HashMap<>();
        highest.put("region", null);
        highest.put("density", null);

        Map<String, Object> lowest = new HashMap<>();
        lowest.put("region", null);
        lowest.put("density", null);

        pd.put("highest", highest);
        pd.put("lowest", lowest);
        return pd;
    }

    private Map<String, List<String>> buildInfrastructureOverview(List<POI> pois, List<String> ports) {
        Map<String, List<String>> infra = new HashMap<>();

        List<String> energy = pois.stream()
                .filter(p -> {
                    String desc = Optional.ofNullable(p.getDescription()).orElse("").toLowerCase();
                    return desc.contains("电厂") || desc.contains("power") || desc.contains("energy");
                })
                .map(POI::getName)
                .distinct()
                .collect(Collectors.toList());

        List<String> airports = pois.stream()
                .filter(p -> {
                    String name = Optional.ofNullable(p.getName()).orElse("").toLowerCase();
                    String desc = Optional.ofNullable(p.getDescription()).orElse("").toLowerCase();
                    return name.contains("机场") || desc.contains("airport");
                })
                .map(POI::getName)
                .distinct()
                .collect(Collectors.toList());

        List<String> transport = new ArrayList<>();
        transport.addAll(ports);
        transport.addAll(airports);

        infra.put("energy", energy);
        infra.put("transport", transport);
        infra.put("communication", Collections.emptyList());
        return infra;
    }
}


