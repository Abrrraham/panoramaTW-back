package com.panorama.backend.service.map;

import com.panorama.backend.DTO.InfoDTO;
import com.panorama.backend.annotation.DynamicNodeData;
import com.panorama.backend.mapper.VectorTileMapper;
import com.panorama.backend.model.Constant.GenerateResultStatus;
import com.panorama.backend.model.Constant.LayerStatus;
import com.panorama.backend.model.node.LayerNode;
import com.panorama.backend.model.resource.DefaultDataSource;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.service.node.LayerNodeService;
import com.panorama.backend.util.FileUtil;
import com.panorama.backend.util.JsonUtil;
import com.panorama.backend.util.ProcessUtil;
import com.panorama.backend.util.ShapeFileUtil;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author: DMK
 * @description:
 * @date: 2024-09-02 09:46:30
 * @version: 1.0
 */
@Service
@Slf4j
public class VectorTileService {
    private VectorTileMapper vectorTileMapper;

    @Value("${path.temp}")
    private String temp;

    private DefaultDataSource defaultDataSource;

    private LayerNodeService layerNodeService;

    private static final long MAX_UPLOAD_BYTES = 1024L * 1024L * 1024L;

    @Autowired
    public void setVectorTileMapper(VectorTileMapper vectorTileMapper, LayerNodeService layerNodeService, DefaultDataSource defaultDataSource) {
        this.vectorTileMapper = vectorTileMapper;
        this.layerNodeService = layerNodeService;
        this.defaultDataSource = defaultDataSource;
    }

    @DynamicNodeData
    public byte[] getVectorTile(LayerNode layerNode, int z, int x, int y) {
        String tableName = layerNode.getTableName();
        ensureOgcFidColumn(tableName);
        String visualizationFields = layerNode.getUsage() == null ? "" : layerNode.getUsage().getOrDefault("visualizationField", "");
        String[] visualizationFieldsList = visualizationFields.isBlank() ? new String[0] : visualizationFields.split(",");
        byte[] tile = (byte[]) vectorTileMapper.getVectorTile(tableName, z, x, y, visualizationFieldsList);
        return tile;
    }

    @DynamicNodeData
    public JsonNode getDetailInfo(LayerNode layerNode, int ogc_fid) {
        String tableName = layerNode.getTableName();
        ensureOgcFidColumn(tableName);
        String detailFields = layerNode.getUsage() == null ? "" : layerNode.getUsage().getOrDefault("detailField", "");
        String[] detailFieldsList = detailFields.isBlank() ? new String[0] : detailFields.split(",");
        return JsonUtil.mapToJson(vectorTileMapper.getDetailInfo(tableName, ogc_fid, detailFieldsList));
    }

    @DynamicNodeData
    public GeneralResult uploadJSONLayer(LayerNode parentNode, MultipartFile multipartFile, InfoDTO infoDTO) throws IOException {
        if (!vectorTileMapper.hasPostgis()) {
            return GeneralResult.builder()
                    .code("POSTGIS_REQUIRED")
                    .status(GenerateResultStatus.ERROR)
                    .message("postgis extension is not enabled")
                    .build();
        }
        if (parentNode == null) {
            return GeneralResult.builder()
                    .code("PARENT_NOT_FOUND")
                    .status(GenerateResultStatus.ERROR)
                    .message("parent node not found")
                    .build();
        }
        String originalName = multipartFile.getOriginalFilename();
        if (!FileUtil.hasAllowedExtension(originalName, new String[]{"json", "geojson"})) {
            return GeneralResult.builder()
                    .code("INVALID_EXTENSION")
                    .status(GenerateResultStatus.ERROR)
                    .message("only .json or .geojson is allowed")
                    .build();
        }
        if (multipartFile.getSize() > MAX_UPLOAD_BYTES) {
            return GeneralResult.builder()
                    .code("FILE_TOO_LARGE")
                    .status(GenerateResultStatus.ERROR)
                    .message("file too large")
                    .build();
        }
        File file = FileUtil.convertMultipartFileToFile(multipartFile, temp);
        String uniqueTableName = null;
        try {
            JsonNode root = JsonUtil.parseJson(file);
            JsonNode features = root == null ? null : root.get("features");
            if (features == null || !features.isArray()) {
                return GeneralResult.builder()
                        .code("INVALID_GEOJSON")
                        .status(GenerateResultStatus.ERROR)
                        .message("invalid geojson")
                        .build();
            }

            String tableName = FileUtil.sanitizeTableName(infoDTO.getTableName());
            if (tableName.isBlank()) {
                return GeneralResult.builder()
                        .code("INVALID_TABLE")
                        .status(GenerateResultStatus.ERROR)
                        .message("table name required")
                        .build();
            }
            String layerName = infoDTO.getLayerName();
            if (layerName == null || layerName.isBlank()) {
                return GeneralResult.builder()
                        .code("INVALID_LAYER_NAME")
                        .status(GenerateResultStatus.ERROR)
                        .message("layer name required")
                        .build();
            }

            int sameCount = vectorTileMapper.getSameCount(tableName);
            if (sameCount == 0){
                uniqueTableName = tableName;
            }else {
                uniqueTableName = tableName + "_" + sameCount;
            }

            Map<String, String> usage = infoDTO.getUsage() == null ? new HashMap<>() : new HashMap<>(infoDTO.getUsage());
            int srid = parseSrid(usage);
            String vectorType = normalizeVectorType(usage.get("type"), features);

            Map<String, String> columnMapping = new HashMap<>();
            Map<String, Object> propertyType = inferPropertyTypes(features, columnMapping);
            vectorTileMapper.createTable(uniqueTableName, propertyType, "Geometry", srid);
            ensureOgcFidColumn(uniqueTableName);

            for (JsonNode feature : features) {
                JsonNode geometryNode = feature.get("geometry");
                if (geometryNode == null || geometryNode.isNull()) {
                    continue;
                }
                String geometry = geometryNode.toString();
                JsonNode propsNode = feature.get("properties");
                Map<String, Object> properties = propsNode == null ? new HashMap<>() : JsonUtil.jsonToMap(propsNode);
                Map<String, Object> sanitizedProps = sanitizeProperties(properties, columnMapping);
                vectorTileMapper.insertGeoJsonFeature(uniqueTableName, geometry, sanitizedProps, srid);
            }

            Map<String, String> dataSourceMap = getDataSourceMap();
            usage.put("category", "vector");
            usage.put("srid", String.valueOf(srid));
            usage.put("format", "geojson");
            usage.put("status", LayerStatus.READY);
            usage.put("dataRef", uniqueTableName);
            usage.put("type", vectorType);
            usage.putIfAbsent("visualizationField", pickDefaultField(propertyType));
            usage.putIfAbsent("detailField", pickDefaultField(propertyType));
            usage.put("errorMessage", "");

            String extent = vectorTileMapper.getLayerBBox(uniqueTableName, 4326);
            String bbox = parseExtent(extent);
            if (bbox != null) {
                usage.put("bbox", bbox);
            }

            LayerNode newLayerNode = LayerNode.builder()
                    .tableName(uniqueTableName).layerName(layerName)
                    .category("vector").usage(usage)
                    .path(layerNodeService.getNodePath(parentNode)).dataSource(dataSourceMap)
                    .createdAt(System.currentTimeMillis())
                    .updatedAt(System.currentTimeMillis())
                    .build();

            layerNodeService.saveLayerNode(newLayerNode);
            FileUtil.deleteDirectory(Path.of(file.getParent()));

            return GeneralResult.builder()
                    .status(GenerateResultStatus.SUCCESS)
                    .message("upload json successfully")
                    .data(Map.of("layerNodeId", newLayerNode.getId()))
                    .build();

        } catch (Exception e){
            if (uniqueTableName != null) {
                vectorTileMapper.deleteTable(uniqueTableName);
            }
            FileUtil.deleteDirectory(Path.of(file.getParent()));
            return GeneralResult.builder()
                    .code("UPLOAD_JSON_FAILED")
                    .status(GenerateResultStatus.ERROR)
                    .message("invalid json")
                    .data(e.getMessage())
                    .build();
        }
    }

    public GeneralResult parseShpLayer(MultipartFile multipartFile) throws IOException {
        if (!vectorTileMapper.hasPostgis()) {
            return GeneralResult.builder()
                    .code("POSTGIS_REQUIRED")
                    .status(GenerateResultStatus.ERROR)
                    .message("postgis extension is not enabled")
                    .build();
        }
        String originalName = multipartFile.getOriginalFilename();
        if (!FileUtil.hasAllowedExtension(originalName, new String[]{"zip"})) {
            return GeneralResult.builder()
                    .code("INVALID_EXTENSION")
                    .status(GenerateResultStatus.ERROR)
                    .message("only .zip is allowed for shp upload")
                    .build();
        }
        if (multipartFile.getSize() > MAX_UPLOAD_BYTES) {
            return GeneralResult.builder()
                    .code("FILE_TOO_LARGE")
                    .status(GenerateResultStatus.ERROR)
                    .message("file too large")
                    .build();
        }

        File zipFile = FileUtil.convertMultipartFileToFile(multipartFile, temp);
        String path = zipFile.getParent();
        List<String> list = FileUtil.unZipFiles(zipFile, path);
        boolean result = zipFile.delete();
        if (!result) {
            log.error("failed to delete zip file");
        }

        Map<String, Object> validation = ShapeFileUtil.validateShapefiles(list);
        Object valid = validation.get("isValid");
        if (!(valid instanceof Boolean) || !((Boolean) valid)) {
            FileUtil.deleteDirectory(Path.of(path));
            return GeneralResult.builder()
                    .code("SHP_COMPONENT_MISSING")
                    .status(GenerateResultStatus.ERROR)
                    .message("shapefile components missing")
                    .data(validation)
                    .build();
        }

        String shapefilePath = ShapeFileUtil.findMainShpFile(list);
        if (shapefilePath == null || shapefilePath.isEmpty()) {
            FileUtil.deleteDirectory(Path.of(path));
            return GeneralResult.builder()
                    .code("SHP_NOT_FOUND")
                    .status(GenerateResultStatus.ERROR)
                    .message("shapefile not found")
                    .build();
        }

        try {
            Map<String, Object> info = new HashMap<>();
            Map<String, String> fields = ShapeFileUtil.parseShapefile(new File(shapefilePath));
            Map<String, String> propertyType = new HashMap<>(fields);
            propertyType.remove("the_geom");
            info.put("propertyType", propertyType);
            info.put("path", path);
            info.put("hasProjection", validation.get("hasProjection"));
            try {
                Map<String, Object> shpInfo = ShapeFileUtil.getShapefileInfo(new File(shapefilePath));
                info.put("geometryType", shpInfo.get("geometryType"));
                info.put("bounds", shpInfo.get("bounds"));
            } catch (Exception e) {
                info.put("geometryType", "");
            }
            return GeneralResult.builder().status(GenerateResultStatus.SUCCESS).message(info).build();
        } catch (Exception e) {
            FileUtil.deleteDirectory(Path.of(path));
            return GeneralResult.builder()
                    .code("SHP_PARSE_FAILED")
                    .status(GenerateResultStatus.ERROR)
                    .message("failed to parse shp")
                    .data(e.getMessage())
                    .build();
        }
    }

    @DynamicNodeData
    public GeneralResult storeShpLayer(LayerNode parentNode, String path, InfoDTO infoDTO) throws IOException, InterruptedException, FactoryException {
        if (!vectorTileMapper.hasPostgis()) {
            return GeneralResult.builder()
                    .code("POSTGIS_REQUIRED")
                    .status(GenerateResultStatus.ERROR)
                    .message("postgis extension is not enabled")
                    .build();
        }
        if (parentNode == null) {
            return GeneralResult.builder()
                    .code("PARENT_NOT_FOUND")
                    .status(GenerateResultStatus.ERROR)
                    .message("parent node not found")
                    .build();
        }
        String shpPath = FileUtil.findFileWithExtensionIgnoreCase(path, ".shp");
        if (shpPath == null || shpPath.isEmpty()) {
            return GeneralResult.builder()
                    .code("SHP_NOT_FOUND")
                    .status(GenerateResultStatus.ERROR)
                    .message("shapefile not found")
                    .build();
        }
        String prjPath = FileUtil.findFileWithExtensionIgnoreCase(path, ".prj");

        String wkt = null;
        Integer srid = null;
        if (prjPath != null) {
            wkt = new String(Files.readAllBytes(Paths.get(prjPath)));
            CoordinateReferenceSystem crs = CRS.parseWKT(wkt);
            srid = CRS.lookupEpsgCode(crs, false);
        }
        Map<String, String> usage = infoDTO.getUsage() == null ? new HashMap<>() : new HashMap<>(infoDTO.getUsage());
        if (srid == null || srid <= 0) {
            String sridValue = usage.get("srid");
            if (sridValue == null || sridValue.isBlank()) {
                return GeneralResult.builder()
                        .code("SRID_REQUIRED")
                        .status(GenerateResultStatus.ERROR)
                        .message("srid required when .prj is missing")
                        .build();
            }
            try {
                srid = Integer.parseInt(sridValue.trim());
            } catch (NumberFormatException e) {
                return GeneralResult.builder()
                        .code("INVALID_SRID")
                        .status(GenerateResultStatus.ERROR)
                        .message("invalid srid")
                        .build();
            }
        }

        String tableName = FileUtil.sanitizeTableName(infoDTO.getTableName());
        if (tableName.isBlank()) {
            return GeneralResult.builder()
                    .code("INVALID_TABLE")
                    .status(GenerateResultStatus.ERROR)
                    .message("table name required")
                    .build();
        }
        int sameCount = vectorTileMapper.getSameCount(tableName);
        String uniqueTableName;
        if (sameCount == 0){
            uniqueTableName = tableName;
        }else {
            uniqueTableName = tableName + "_" + sameCount;
        }
        String layerName = infoDTO.getLayerName();
        if (layerName == null || layerName.isBlank()) {
            return GeneralResult.builder()
                    .code("INVALID_LAYER_NAME")
                    .status(GenerateResultStatus.ERROR)
                    .message("layer name required")
                    .build();
        }

        if (ProcessUtil.shp2pgProcess(shpPath, uniqueTableName, defaultDataSource, srid)){
            ensureOgcFidColumn(uniqueTableName);
            Map<String, String> dataSourceMap = getDataSourceMap();
            usage.put("category", "vector");
            usage.put("srid", String.valueOf(srid));
            usage.put("format", "shp");
            usage.put("status", LayerStatus.READY);
            usage.put("dataRef", uniqueTableName);
            usage.put("errorMessage", "");

            String geometryType = null;
            try {
                Map<String, Object> shpInfo = ShapeFileUtil.getShapefileInfo(new File(shpPath));
                if (shpInfo != null) {
                    Object g = shpInfo.get("geometryType");
                    geometryType = g == null ? null : g.toString();
                }
            } catch (Exception e) {
                geometryType = null;
            }
            String vectorType = normalizeVectorType(usage.get("type"), geometryType);
            usage.put("type", vectorType);

            Map<String, String> fields = ShapeFileUtil.parseShapefile(new File(shpPath));
            fields.remove("the_geom");
            usage.putIfAbsent("visualizationField", pickDefaultField(fields));
            usage.putIfAbsent("detailField", pickDefaultField(fields));

            String extent = vectorTileMapper.getLayerBBox(uniqueTableName, 4326);
            String bbox = parseExtent(extent);
            if (bbox != null) {
                usage.put("bbox", bbox);
            }

            LayerNode newLayerNode = LayerNode.builder()
                    .tableName(uniqueTableName).layerName(layerName)
                    .category("vector").usage(usage)
                    .path(layerNodeService.getNodePath(parentNode)).dataSource(dataSourceMap)
                    .createdAt(System.currentTimeMillis())
                    .updatedAt(System.currentTimeMillis())
                    .build();
            layerNodeService.saveLayerNode(newLayerNode);
            FileUtil.deleteDirectory(Path.of(path));
            return GeneralResult.builder()
                    .status(GenerateResultStatus.SUCCESS)
                    .message("store shp successfully")
                    .data(Map.of("layerNodeId", newLayerNode.getId()))
                    .build();
        }else {
            String errorDetail = ProcessUtil.getLastProcessError();
            return GeneralResult.builder()
                    .code("SHP_STORE_FAILED")
                    .status(GenerateResultStatus.ERROR)
                    .message("failed to store shp")
                    .data(errorDetail)
                    .build();
        }
    }

    @DynamicNodeData
    public GeneralResult deleteVectorLayer(LayerNode layerNode){
        try{
            vectorTileMapper.deleteTable(layerNode.getTableName());
            layerNodeService.deleteLayerNode(layerNode);
            return GeneralResult.builder().status(GenerateResultStatus.SUCCESS).message("delete vector layer successfully").build();
        }catch (Exception e){
            return GeneralResult.builder()
                    .code("VECTOR_DELETE_FAILED")
                    .status(GenerateResultStatus.ERROR)
                    .message("failed to delete vector layer")
                    .data(e.getMessage())
                    .build();
        }
    }

    @DynamicNodeData
    public GeneralResult updateVectorLayer(LayerNode layerNode, InfoDTO infoDTO){
        if (layerNodeService.updateLayer(layerNode, infoDTO)){
            return GeneralResult.builder().status(GenerateResultStatus.SUCCESS).message("update vector layer successfully").build();
        }else{
            return GeneralResult.builder().status(GenerateResultStatus.ERROR).message("failed to update vector layer").build();
        }
    }

    @DynamicNodeData
    public GeneralResult getGeojsonByTableName(LayerNode layerNode, String tableName) {
        try {
            return GeneralResult.builder().status(GenerateResultStatus.SUCCESS).message(vectorTileMapper.getGeojsonByTableName(tableName)).build();
        } catch (Exception e) {
            return GeneralResult.builder().status(GenerateResultStatus.ERROR).message("failed to get geojson").build();
        }
    }

    // 新增：获取所有列
    @DynamicNodeData
    public List<String> getAllColumns(LayerNode layerNode) {
        return vectorTileMapper.getAllColumns(layerNode.getTableName());
    }

    // 新增：分页获取属性
    @DynamicNodeData
    public List<Map<String, Object>> getAttributes(LayerNode layerNode, List<String> columns, int page, int size) {
        ensureOgcFidColumn(layerNode.getTableName());
        int offset = Math.max(0, (page - 1) * size);
        return vectorTileMapper.getAttributes(layerNode.getTableName(), columns, offset, size);
    }

    // 新增：总数
    @DynamicNodeData
    public int getRowCount(LayerNode layerNode) {
        return vectorTileMapper.getRowCount(layerNode.getTableName());
    }
    
    // 临时：调试用
    public List<String> findTablesLikePort() {
        return vectorTileMapper.findTablesLikePort();
    }

    private int parseSrid(Map<String, String> usage) {
        if (usage == null) {
            return 4326;
        }
        String sridValue = usage.get("srid");
        if (sridValue == null || sridValue.isBlank()) {
            return 4326;
        }
        try {
            return Integer.parseInt(sridValue.trim());
        } catch (NumberFormatException e) {
            return 4326;
        }
    }

    private String normalizeVectorType(String preferred, Object geometrySource) {
        String type = preferred;
        if (type == null || type.isBlank()) {
            if (geometrySource instanceof JsonNode node) {
                type = inferGeometryTypeFromFeatures(node);
            } else if (geometrySource != null) {
                type = geometrySource.toString();
            }
        }
        return normalizeVectorTypeValue(type);
    }

    private String inferGeometryTypeFromFeatures(JsonNode features) {
        if (features == null || !features.isArray()) {
            return null;
        }
        for (JsonNode feature : features) {
            JsonNode geometry = feature.get("geometry");
            if (geometry != null && geometry.has("type")) {
                String type = geometry.get("type").asText();
                if (type != null && !type.isBlank()) {
                    return type;
                }
            }
        }
        return null;
    }

    private String normalizeVectorTypeValue(String type) {
        if (type == null || type.isBlank()) {
            return "polygon";
        }
        String lower = type.toLowerCase(Locale.ROOT);
        if (lower.contains("point")) {
            return "point";
        }
        if (lower.contains("line")) {
            return "line";
        }
        if (lower.contains("poly")) {
            return "polygon";
        }
        return "polygon";
    }

    private Map<String, Object> inferPropertyTypes(JsonNode features, Map<String, String> columnMapping) {
        Map<String, Object> propertyType = new HashMap<>();
        if (features == null || !features.isArray()) {
            return propertyType;
        }
        for (JsonNode feature : features) {
            JsonNode props = feature.get("properties");
            if (props == null || !props.isObject()) {
                continue;
            }
            props.fields().forEachRemaining(entry -> {
                String originalKey = entry.getKey();
                String column = columnMapping.get(originalKey);
                if (column == null) {
                    column = normalizeColumnName(originalKey);
                    if (column.isBlank() || "geom".equalsIgnoreCase(column) || "the_geom".equalsIgnoreCase(column)) {
                        return;
                    }
                    String base = column;
                    int suffix = 1;
                    while (propertyType.containsKey(column)) {
                        column = base + "_" + suffix;
                        suffix++;
                    }
                    columnMapping.put(originalKey, column);
                }
                String sqlType = inferSqlType(entry.getValue());
                Object existing = propertyType.get(column);
                if (existing == null) {
                    propertyType.put(column, sqlType);
                } else {
                    propertyType.put(column, mergeSqlType(existing.toString(), sqlType));
                }
            });
        }
        return propertyType;
    }

    private String inferSqlType(JsonNode value) {
        if (value == null || value.isNull()) {
            return "text";
        }
        if (value.isBoolean()) {
            return "boolean";
        }
        if (value.isInt() || value.isLong()) {
            return "bigint";
        }
        if (value.isFloatingPointNumber()) {
            return "double precision";
        }
        if (value.isObject() || value.isArray()) {
            return "jsonb";
        }
        return "text";
    }

    private String mergeSqlType(String existing, String next) {
        if (existing.equals(next)) {
            return existing;
        }
        if ("jsonb".equals(existing) || "jsonb".equals(next)) {
            return "jsonb";
        }
        return "text";
    }

    private Map<String, Object> sanitizeProperties(Map<String, Object> properties, Map<String, String> columnMapping) {
        Map<String, Object> sanitized = new HashMap<>();
        if (properties == null) {
            return sanitized;
        }
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String column = columnMapping.get(entry.getKey());
            if (column != null && !column.isBlank()) {
                sanitized.put(column, entry.getValue());
            }
        }
        return sanitized;
    }

    private String normalizeColumnName(String key) {
        if (key == null) {
            return "";
        }
        String cleaned = key.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase(Locale.ROOT);
        if (cleaned.isBlank()) {
            return "";
        }
        if (Character.isDigit(cleaned.charAt(0))) {
            cleaned = "f_" + cleaned;
        }
        return cleaned;
    }

    private String pickDefaultField(Map<String, ?> fields) {
        if (fields == null || fields.isEmpty()) {
            return "";
        }
        return fields.keySet().iterator().next();
    }

    private String parseExtent(String extent) {
        if (extent == null || extent.isBlank()) {
            return null;
        }
        String clean = extent.replace("BOX(", "").replace(")", "");
        String[] parts = clean.split(",");
        if (parts.length != 2) {
            return null;
        }
        String[] min = parts[0].trim().split("\\s+");
        String[] max = parts[1].trim().split("\\s+");
        if (min.length < 2 || max.length < 2) {
            return null;
        }
        return String.format(Locale.ROOT, "[%s,%s,%s,%s]", min[0], min[1], max[0], max[1]);
    }

    private Map<String, String> getDataSourceMap() {
        Map<String, String> dataSourceMap = new HashMap<>();
        dataSourceMap.put("url", defaultDataSource.getUrl());
        dataSourceMap.put("username", defaultDataSource.getUsername());
        dataSourceMap.put("password", defaultDataSource.getPassword());
        return dataSourceMap;
    }

    private void ensureOgcFidColumn(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return;
        }
        try {
            if (vectorTileMapper.hasColumn(tableName, "ogc_fid")) {
                return;
            }
            if (vectorTileMapper.hasColumn(tableName, "gid")) {
                vectorTileMapper.renameGidToOgcFid(tableName);
                return;
            }
            if (vectorTileMapper.hasColumn(tableName, "id")) {
                vectorTileMapper.renameIdToOgcFid(tableName);
            }
        } catch (Exception e) {
            log.warn("failed to ensure ogc_fid for table {}: {}", tableName, e.getMessage());
        }
    }

}
