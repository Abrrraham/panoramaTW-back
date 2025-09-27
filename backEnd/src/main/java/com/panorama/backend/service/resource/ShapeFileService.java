package com.panorama.backend.service.resource;

import com.panorama.backend.DTO.InfoDTO;
import com.panorama.backend.model.Constant.GenerateResultStatus;
import com.panorama.backend.model.Constant.TaskStatus;
import com.panorama.backend.model.Constant.TaskType;
import com.panorama.backend.model.node.LayerNode;
import com.panorama.backend.model.node.ModelNode;
import com.panorama.backend.model.node.TaskNode;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.service.node.LayerNodeService;
import com.panorama.backend.service.node.ModelNodeService;
import com.panorama.backend.service.node.TaskNodeService;
import com.panorama.backend.util.FileUtil;
import com.panorama.backend.util.ShapeFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * SHP文件处理服务类
 * 负责SHP文件的解析、转换和存储
 * 
 * @author: Panorama System
 * @description: SHP文件导入的核心业务逻辑
 * @date: 2024-09-27
 * @version: 1.0
 */
@Service
@Slf4j
public class ShapeFileService {

    @Value("${path.temp}")
    private String tempPath;

    @Value("${path.static}")
    private String staticPath;

    @Value("${path.vector:${path.static}/vector}")
    private String vectorPath;

    private LayerNodeService layerNodeService;
    private TaskNodeService taskNodeService;
    private ModelNodeService modelNodeService;
    private AsyncTaskService asyncTaskService;
    
    // 用于防止重复处理同一文件的并发Map
    private final ConcurrentHashMap<String, Long> processingFiles = new ConcurrentHashMap<>();

    @Autowired
    public void setShapeFileService(LayerNodeService layerNodeService, TaskNodeService taskNodeService, 
                                  ModelNodeService modelNodeService, AsyncTaskService asyncTaskService) {
        this.layerNodeService = layerNodeService;
        this.taskNodeService = taskNodeService;
        this.modelNodeService = modelNodeService;
        this.asyncTaskService = asyncTaskService;
    }

    /**
     * 上传并处理SHP文件
     * 
     * @param file 上传的文件
     * @param infoDTO 图层信息
     * @return 处理结果
     */
    public GeneralResult uploadShapeFile(MultipartFile file, InfoDTO infoDTO) {
        // 参数验证
        if (file == null || file.isEmpty()) {
            log.warn("上传的文件为空或null");
            return GeneralResult.builder()
                    .status(GenerateResultStatus.ERROR)
                    .message("上传的文件不能为空")
                    .build();
        }
        
        if (infoDTO == null) {
            log.warn("图层信息为null");
            return GeneralResult.builder()
                    .status(GenerateResultStatus.ERROR)
                    .message("图层信息不能为空")
                    .build();
        }
        
        if (infoDTO.getTableName() == null || infoDTO.getTableName().trim().isEmpty()) {
            log.warn("表名为空，使用默认表名");
            infoDTO.setTableName("layer_" + System.currentTimeMillis());
        }
        
        if (infoDTO.getLayerName() == null || infoDTO.getLayerName().trim().isEmpty()) {
            log.warn("图层名为空，使用默认图层名");
            infoDTO.setLayerName("未命名图层_" + System.currentTimeMillis());
        }
        
        // 声明需要在多个作用域中使用的变量
        File zipFile = null;
        String parentPath = null;
        List<String> extractedFiles = null;
        
        try {
            log.info("开始处理文件上传: 文件名={}, 大小={}bytes, 表名={}, 图层名={}", 
                    file.getOriginalFilename(), file.getSize(), infoDTO.getTableName(), infoDTO.getLayerName());
            
            // 1. 验证文件
            GeneralResult validationResult = validateShapeFile(file);
            if (!GenerateResultStatus.SUCCESS.equals(validationResult.getStatus())) {
                return validationResult;
            }

            // 2. 转换MultipartFile为File并解压
            
            try {
                zipFile = FileUtil.convertMultipartFileToFile(file, tempPath);
                parentPath = zipFile.getParent();
                extractedFiles = FileUtil.unZipFiles(zipFile, parentPath);
                log.info("文件解压成功，解压文件数量: {}", extractedFiles.size());
                
            } catch (Exception e) {
                log.error("文件解压失败: {}", e.getMessage(), e);
                // 清理已创建的文件
                if (zipFile != null && zipFile.exists()) {
                    zipFile.delete();
                }
                if (parentPath != null) {
                    FileUtil.deleteDirectory(Path.of(parentPath));
                }
                return GeneralResult.builder()
                        .status(GenerateResultStatus.ERROR)
                        .message("文件解压失败: " + e.getMessage())
                        .build();
            } finally {
                // 清理zip文件
                if (zipFile != null && zipFile.exists() && !zipFile.delete()) {
                    log.warn("Failed to delete zip file: {}", zipFile.getPath());
                }
            }

            // 3. 查找支持的地理数据文件
            String geoDataFilePath = "";
            String fileType = "";
            
            // 优先查找.shp文件
            geoDataFilePath = FileUtil.findFileWithExtension(extractedFiles, "shp");
            if (!geoDataFilePath.isEmpty()) {
                fileType = "shp";
            } else {
                // 如果没有.shp文件，查找其他支持的格式
                geoDataFilePath = FileUtil.findFileWithExtension(extractedFiles, "geojson");
                if (!geoDataFilePath.isEmpty()) {
                    fileType = "geojson";
                } else {
                    geoDataFilePath = FileUtil.findFileWithExtension(extractedFiles, "kml");
                    if (!geoDataFilePath.isEmpty()) {
                        fileType = "kml";
                    } else {
                        geoDataFilePath = FileUtil.findFileWithExtension(extractedFiles, "gpx");
                        if (!geoDataFilePath.isEmpty()) {
                            fileType = "gpx";
                        }
                    }
                }
            }
            
            if (geoDataFilePath.isEmpty()) {
                return GeneralResult.builder()
                        .status(GenerateResultStatus.ERROR)
                        .message("在上传的文件中未找到支持的地理数据文件（.shp, .geojson, .kml, .gpx）")
                        .build();
            }
            
            log.info("找到地理数据文件: {}, 类型: {}", geoDataFilePath, fileType);

            // 4. 解析地理数据文件
            File geoDataFile = new File(geoDataFilePath);
            Map<String, Object> geoInfo;
            
            if ("shp".equals(fileType)) {
                geoInfo = parseShapeFileInfo(geoDataFile);
            } else {
                // 对于其他格式，创建基本信息
                geoInfo = new HashMap<>();
                geoInfo.put("featureCount", 0);
                geoInfo.put("geometryType", "Unknown");
                geoInfo.put("fields", new ArrayList<>());
                log.info("暂不支持 {} 格式的详细解析，使用默认信息", fileType);
            }
            
            // 5. 转换为GeoJSON并保存
            String tableName = infoDTO.getTableName();
            long sameCount = FileUtil.countFilesWithPrefix(vectorPath, tableName);
            String uniqueTableName = sameCount == 0 ? tableName : tableName + "_" + sameCount;
            
            String geoJsonPath;
            if ("shp".equals(fileType)) {
                geoJsonPath = convertShpToGeoJson(geoDataFile, vectorPath, uniqueTableName);
            } else if ("geojson".equals(fileType)) {
                // 如果已经是GeoJSON，直接复制到目标位置
                geoJsonPath = copyGeoJsonFile(geoDataFile, vectorPath, uniqueTableName);
            } else {
                // 其他格式暂时不支持转换
                return GeneralResult.builder()
                        .status(GenerateResultStatus.ERROR)
                        .message("暂不支持 " + fileType + " 格式的转换")
                        .build();
            }
            
            // 6. 创建图层节点
            LayerNode parentNode = layerNodeService.getLayerNodeById(infoDTO.getParent_id());
            LayerNode layerNode = createShapeFileLayerNode(parentNode, uniqueTableName, infoDTO, geoInfo, geoJsonPath);
            
            // 7. 保存图层节点
            layerNodeService.saveLayerNode(layerNode);
            
            // 8. 清理临时文件
            try {
                if (parentPath != null) {
                    FileUtil.deleteDirectory(Path.of(parentPath));
                    log.info("临时文件清理完成: {}", parentPath);
                }
            } catch (Exception e) {
                log.warn("临时文件清理失败: {}", e.getMessage());
            }
            
            log.info("文件处理完成: {}", uniqueTableName);
            return GeneralResult.builder()
                    .status(GenerateResultStatus.SUCCESS)
                    .message("文件导入成功")
                    .data(Map.of(
                            "layerId", layerNode.getId(),
                            "tableName", uniqueTableName,
                            "featureCount", geoInfo.get("featureCount"),
                            "geometryType", geoInfo.get("geometryType"),
                            "bounds", geoInfo.get("bounds"),
                            "fileType", fileType
                    ))
                    .build();
                    
        } catch (Exception e) {
            log.error("文件处理失败: {}", e.getMessage(), e);
            
            // 确保临时文件被清理
            try {
                if (parentPath != null) {
                    FileUtil.deleteDirectory(Path.of(parentPath));
                    log.info("异常情况下临时文件清理完成: {}", parentPath);
                }
            } catch (Exception cleanupException) {
                log.warn("异常情况下临时文件清理失败: {}", cleanupException.getMessage());
            }
            
            return GeneralResult.builder()
                    .status(GenerateResultStatus.ERROR)
                    .message("文件处理失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 验证SHP文件格式
     * 
     * @param file 待验证的文件
     * @return 验证结果
     */
    public GeneralResult validateShapeFile(MultipartFile file) {
        String fileKey = null;
        try {
            log.info("开始验证文件: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());
            
            if (file.isEmpty()) {
                log.warn("上传的文件为空");
                return GeneralResult.builder()
                        .status(GenerateResultStatus.ERROR)
                        .message("上传的文件为空")
                        .build();
            }

            String filename = file.getOriginalFilename();
            log.info("文件名: {}", filename);
            
            // 生成文件的唯一标识，用于去重
            fileKey = generateFileKey(filename, file.getSize());
            
            // 检查是否有相同文件正在处理
            Long currentTime = System.currentTimeMillis();
            Long existingTime = processingFiles.putIfAbsent(fileKey, currentTime);
            if (existingTime != null) {
                log.warn("文件 {} 正在被处理中，拒绝重复请求", filename);
                return GeneralResult.builder()
                        .status(GenerateResultStatus.ERROR)
                        .message("该文件正在处理中，请稍后再试")
                        .build();
            }
            
            log.info("开始处理文件: {}, 处理键: {}", filename, fileKey);
            
            if (filename == null || (!filename.toLowerCase().endsWith(".zip") && !filename.toLowerCase().endsWith(".shp"))) {
                log.warn("文件格式不支持: {}", filename);
                return GeneralResult.builder()
                        .status(GenerateResultStatus.ERROR)
                        .message("请上传.zip压缩包或.shp文件")
                        .build();
            }

            // 如果是ZIP文件，检查是否包含必需的SHP文件组件
            if (filename.toLowerCase().endsWith(".zip")) {
                log.info("处理ZIP文件: {}", filename);
                File tempFile = FileUtil.convertMultipartFileToFile(file, tempPath);
                String parentPath = tempFile.getParent();
                log.info("临时文件路径: {}", tempFile.getAbsolutePath());
                
                try {
                    List<String> extractedFiles = FileUtil.unZipFiles(tempFile, parentPath);
                    log.info("解压后的文件数量: {}", extractedFiles.size());
                    log.info("解压的文件列表: {}", extractedFiles);
                    
                    boolean hasShp = extractedFiles.stream().anyMatch(f -> f.toLowerCase().endsWith(".shp"));
                    boolean hasShx = extractedFiles.stream().anyMatch(f -> f.toLowerCase().endsWith(".shx"));
                    boolean hasDbf = extractedFiles.stream().anyMatch(f -> f.toLowerCase().endsWith(".dbf"));
                    boolean hasAnyGeoData = hasShp || 
                        extractedFiles.stream().anyMatch(f -> f.toLowerCase().endsWith(".geojson")) ||
                        extractedFiles.stream().anyMatch(f -> f.toLowerCase().endsWith(".kml")) ||
                        extractedFiles.stream().anyMatch(f -> f.toLowerCase().endsWith(".gpx"));
                    
                    log.info("文件检查结果 - .shp: {}, .shx: {}, .dbf: {}, 包含地理数据: {}", hasShp, hasShx, hasDbf, hasAnyGeoData);
                    
                    // 如果没有任何地理数据文件，则报错
                    if (!hasAnyGeoData) {
                        log.warn("ZIP文件中未找到支持的地理数据文件");
                        Map<String, Object> errorData = new HashMap<>();
                        errorData.put("isValid", false);
                        errorData.put("missingFiles", Arrays.asList("地理数据文件"));
                        errorData.put("hasProjection", extractedFiles.stream().anyMatch(f -> f.toLowerCase().endsWith(".prj")));
                        errorData.put("foundFiles", extractedFiles.stream().map(f -> new File(f).getName()).collect(java.util.stream.Collectors.toList()));
                        errorData.put("supportedFormats", Arrays.asList(".shp", ".geojson", ".kml", ".gpx"));
                        
                        log.info("返回错误结果，找到的文件: {}", errorData.get("foundFiles"));
                        
                        return GeneralResult.builder()
                                .status(GenerateResultStatus.ERROR)
                                .message("压缩包中未找到支持的地理数据文件（.shp, .geojson, .kml, .gpx）")
                                .data(errorData)
                                .build();
                    }
                    
                    // 如果有.shp文件但缺少关键文件，给出警告但不阻止上传
                    List<String> missingFiles = new ArrayList<>();
                    if (hasShp) {
                        if (!hasShx) missingFiles.add(".shx");
                        if (!hasDbf) missingFiles.add(".dbf");
                    }
                    
                    if (!missingFiles.isEmpty()) {
                        log.warn("ZIP文件缺少推荐的文件: {}，但仍然可以处理", missingFiles);
                    }
                    
                    // 验证成功，返回结果
                    log.info("ZIP文件验证成功");
                    Map<String, Object> validationData = new HashMap<>();
                    validationData.put("isValid", true);
                    validationData.put("missingFiles", new ArrayList<>());
                    validationData.put("hasProjection", extractedFiles.stream().anyMatch(f -> f.toLowerCase().endsWith(".prj")));
                    validationData.put("foundFiles", extractedFiles.stream().map(f -> new File(f).getName()).collect(java.util.stream.Collectors.toList()));
                    
                    log.info("找到的所有文件: {}", validationData.get("foundFiles"));
                    log.info("包含投影文件(.prj): {}", validationData.get("hasProjection"));
                    
                    return GeneralResult.builder()
                            .status(GenerateResultStatus.SUCCESS)
                            .message("文件格式验证通过")
                            .data(validationData)
                            .build();
                    
                } finally {
                    // 清理临时文件
                    tempFile.delete();
                    FileUtil.deleteDirectory(Path.of(parentPath));
                }
            }

            // 如果是单个SHP文件
            log.info("处理单个SHP文件: {}", filename);
            Map<String, Object> validationData = new HashMap<>();
            validationData.put("isValid", true);
            validationData.put("missingFiles", new ArrayList<>());
            validationData.put("hasProjection", false);
            validationData.put("foundFiles", Arrays.asList(filename));
            
            log.info("单个SHP文件验证成功");
            
            return GeneralResult.builder()
                    .status(GenerateResultStatus.SUCCESS)
                    .message("文件格式验证通过")
                    .data(validationData)
                    .build();
                    
        } catch (Exception e) {
            log.error("文件验证失败", e);
            return GeneralResult.builder()
                    .status(GenerateResultStatus.ERROR)
                    .message("文件验证失败: " + e.getMessage())
                    .build();
        } finally {
            // 确保在任何情况下都从处理队列中移除文件
            if (fileKey != null) {
                processingFiles.remove(fileKey);
                log.debug("从处理队列中移除文件: {}", fileKey);
            }
        }
    }
    
    /**
     * 生成文件的唯一标识键，用于去重
     * 
     * @param filename 文件名
     * @param fileSize 文件大小
     * @return 文件唯一标识
     */
    private String generateFileKey(String filename, long fileSize) {
        return String.format("%s_%d", filename != null ? filename : "unknown", fileSize);
    }

    /**
     * 预览SHP文件信息
     * 
     * @param file SHP文件
     * @return 预览信息
     */
    public GeneralResult previewShapeFile(MultipartFile file) {
        try {
            // 先验证文件
            GeneralResult validationResult = validateShapeFile(file);
            if (!GenerateResultStatus.SUCCESS.equals(validationResult.getStatus())) {
                return validationResult;
            }

            // 解压并解析
            File tempFile = FileUtil.convertMultipartFileToFile(file, tempPath);
            String parentPath = tempFile.getParent();
            
            try {
                List<String> extractedFiles = FileUtil.unZipFiles(tempFile, parentPath);
                String shpFilePath = FileUtil.findFileWithExtension(extractedFiles, "shp");
                
                if (shpFilePath.isEmpty()) {
                    return GeneralResult.builder()
                            .status(GenerateResultStatus.ERROR)
                            .message("未找到.shp文件")
                            .build();
                }

                File shpFile = new File(shpFilePath);
                Map<String, Object> shpInfo = parseShapeFileInfo(shpFile);
                
                return GeneralResult.builder()
                        .status(GenerateResultStatus.SUCCESS)
                        .message("文件预览成功")
                        .data(shpInfo)
                        .build();
                        
            } finally {
                tempFile.delete();
                FileUtil.deleteDirectory(Path.of(parentPath));
            }
            
        } catch (Exception e) {
            log.error("文件预览失败", e);
            return GeneralResult.builder()
                    .status(GenerateResultStatus.ERROR)
                    .message("文件预览失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 删除SHP图层
     * 
     * @param layerId 图层ID
     * @return 删除结果
     */
    public GeneralResult deleteShapeFileLayer(String layerId) {
        try {
            LayerNode layerNode = layerNodeService.getLayerNodeById(layerId);
            if (layerNode == null) {
                return GeneralResult.builder()
                        .status(GenerateResultStatus.ERROR)
                        .message("图层不存在")
                        .build();
            }

            // 创建删除任务
            TaskNode taskNode = TaskNode.builder()
                    .status(TaskStatus.NONE)
                    .layerNode(layerNode)
                    .type(TaskType.DELETE)
                    .build();

            String taskNodeId = taskNodeService.saveTaskNode(taskNode);
            
            Map<String, String> params = new HashMap<>();
            String geoJsonPath = layerNode.getDataSource().get("url") + File.separator + 
                                layerNode.getTableName() + ".geojson";
            params.put("path", geoJsonPath);
            
            ModelNode modelNode = modelNodeService.getModelNodeByName("deleteFile");
            taskNode.setParams(params);
            taskNode.setModelNode(modelNode);
            taskNodeService.saveTaskNode(taskNode);
            
            // 异步执行删除任务
            asyncTaskService.systemTaskAsync(taskNodeId);
            
            return GeneralResult.builder()
                    .status(GenerateResultStatus.RUNNING)
                    .message(taskNodeId)
                    .build();
                    
        } catch (Exception e) {
            log.error("删除SHP图层失败", e);
            return GeneralResult.builder()
                    .status(GenerateResultStatus.ERROR)
                    .message("删除失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 解析SHP文件信息
     */
    private Map<String, Object> parseShapeFileInfo(File shpFile) throws IOException {
        Map<String, Object> info = new HashMap<>();
        
        FileDataStore store = FileDataStoreFinder.getDataStore(shpFile);
        SimpleFeatureSource featureSource = store.getFeatureSource();
        SimpleFeatureType schema = featureSource.getSchema();
        SimpleFeatureCollection features = featureSource.getFeatures();
        
        // 基本信息
        info.put("featureCount", features.size());
        info.put("geometryType", schema.getGeometryDescriptor().getType().getName().getLocalPart());
        
        // 坐标系信息
        CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
        if (crs != null) {
            info.put("crs", CRS.toSRS(crs));
            info.put("crsName", crs.getName().toString());
        }
        
        // 空间范围
        ReferencedEnvelope bounds = featureSource.getBounds();
        if (bounds != null) {
            Map<String, Double> boundsMap = new HashMap<>();
            boundsMap.put("minX", bounds.getMinX());
            boundsMap.put("minY", bounds.getMinY());
            boundsMap.put("maxX", bounds.getMaxX());
            boundsMap.put("maxY", bounds.getMaxY());
            info.put("bounds", boundsMap);
        }
        
        // 字段信息
        List<Map<String, String>> fields = new ArrayList<>();
        for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
            if (!descriptor.equals(schema.getGeometryDescriptor())) {
                Map<String, String> field = new HashMap<>();
                field.put("name", descriptor.getLocalName());
                field.put("type", descriptor.getType().getBinding().getSimpleName());
                fields.add(field);
            }
        }
        info.put("fields", fields);
        
        store.dispose();
        return info;
    }

    /**
     * 复制GeoJSON文件到目标位置
     */
    private String copyGeoJsonFile(File geoJsonFile, String outputDir, String tableName) throws IOException {
        // 确保输出目录存在
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        String targetPath = outputDir + File.separator + tableName + ".geojson";
        Files.copy(geoJsonFile.toPath(), Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);
        
        log.info("GeoJSON文件已复制到: {}", targetPath);
        return targetPath;
    }

    /**
     * 将SHP文件转换为GeoJSON
     */
    private String convertShpToGeoJson(File shpFile, String outputDir, String tableName) throws IOException {
        // 确保输出目录存在
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        String geoJsonPath = outputDir + File.separator + tableName + ".geojson";
        
        FileDataStore store = FileDataStoreFinder.getDataStore(shpFile);
        SimpleFeatureSource featureSource = store.getFeatureSource();
        SimpleFeatureCollection features = featureSource.getFeatures();
        
        // 转换为GeoJSON
        FeatureJSON fjson = new FeatureJSON();
        try (FileWriter writer = new FileWriter(geoJsonPath)) {
            writer.write("{\"type\":\"FeatureCollection\",\"features\":[");
            
            boolean first = true;
            try (SimpleFeatureIterator iterator = features.features()) {
                while (iterator.hasNext()) {
                    if (!first) {
                        writer.write(",");
                    }
                    SimpleFeature feature = iterator.next();
                    fjson.writeFeature(feature, writer);
                    first = false;
                }
            }
            
            writer.write("]}");
        }
        
        store.dispose();
        return geoJsonPath;
    }

    /**
     * 创建SHP图层节点
     */
    private LayerNode createShapeFileLayerNode(LayerNode parentNode, String tableName, InfoDTO infoDTO, 
                                             Map<String, Object> shpInfo, String geoJsonPath) {
        // 数据源配置
        Map<String, String> dataSource = new HashMap<>();
        dataSource.put("url", vectorPath);
        dataSource.put("type", "geojson");
        dataSource.put("path", geoJsonPath);
        
        // 使用配置
        Map<String, String> usage = new HashMap<>();
        usage.put("type", "vector");
        usage.put("format", "geojson");
        usage.put("geometryType", (String) shpInfo.get("geometryType"));
        
        // 扩展信息
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("originalFormat", "shapefile");
        metadata.put("featureCount", shpInfo.get("featureCount"));
        metadata.put("fields", shpInfo.get("fields"));
        metadata.put("bounds", shpInfo.get("bounds"));
        metadata.put("crs", shpInfo.get("crs"));
        metadata.put("importTime", System.currentTimeMillis());
        
        return LayerNode.builder()
                .tableName(tableName)
                .layerName(infoDTO.getLayerName())
                .category("vector")
                .usage(usage)
                .dataSource(dataSource)
                .path(layerNodeService.getNodePath(parentNode))
                .build();
    }
}
