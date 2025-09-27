package com.panorama.backend.util;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * SHP文件处理工具类
 * 提供SHP文件解析、转换等功能
 *
 * @author: Steven Da & Panorama System
 * @date: 2024/10/04/18:12
 * @description: SHP文件处理的通用工具方法
 */
public class ShapeFileUtil {
    
    /**
     * 解析Shapefile的字段信息
     * 
     * @param shapefile SHP文件
     * @return 字段名和类型的映射
     * @throws IOException 文件读取异常
     */
    public static Map<String, String> parseShapefile(File shapefile) throws IOException {
        // 存储字段信息
        Map<String, String> fieldInfo = new HashMap<>();

        FileDataStore store = FileDataStoreFinder.getDataStore(shapefile);

        // 2. 获取Shapefile的FeatureSource
        SimpleFeatureSource featureSource = store.getFeatureSource();

        // 3. 获取Schema（Feature Type）
        SimpleFeatureType schema = featureSource.getSchema();

        // 4. 获取字段（属性）的名称列表
        List<AttributeDescriptor> descriptors = schema.getAttributeDescriptors();

        for (AttributeDescriptor descriptor : descriptors) {
            String fieldName = descriptor.getLocalName();
            String fieldType = descriptor.getType().getBinding().getSimpleName();
            fieldInfo.put(fieldName, fieldType);
        }

        store.dispose(); // 关闭资源
        return fieldInfo;
    }

    /**
     * 获取SHP文件的详细信息
     * 
     * @param shapefile SHP文件
     * @return 包含要素数量、几何类型、坐标系、边界等信息的映射
     * @throws IOException 文件读取异常
     */
    public static Map<String, Object> getShapefileInfo(File shapefile) throws IOException {
        Map<String, Object> info = new HashMap<>();
        
        FileDataStore store = FileDataStoreFinder.getDataStore(shapefile);
        SimpleFeatureSource featureSource = store.getFeatureSource();
        SimpleFeatureType schema = featureSource.getSchema();
        SimpleFeatureCollection features = featureSource.getFeatures();
        
        // 基本信息
        info.put("featureCount", features.size());
        info.put("geometryType", schema.getGeometryDescriptor().getType().getName().getLocalPart());
        
        // 坐标系信息
        CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
        if (crs != null) {
            try {
                info.put("crs", CRS.toSRS(crs));
                info.put("crsName", crs.getName().toString());
            } catch (Exception e) {
                info.put("crs", "Unknown");
                info.put("crsName", "Unknown CRS");
            }
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
     * 将SHP文件转换为GeoJSON格式
     * 
     * @param shpFile SHP文件
     * @param outputPath 输出文件路径
     * @return 是否转换成功
     * @throws IOException 文件处理异常
     */
    public static boolean convertToGeoJson(File shpFile, String outputPath) throws IOException {
        FileDataStore store = FileDataStoreFinder.getDataStore(shpFile);
        SimpleFeatureSource featureSource = store.getFeatureSource();
        SimpleFeatureCollection features = featureSource.getFeatures();
        
        // 转换为GeoJSON
        FeatureJSON fjson = new FeatureJSON();
        
        try (FileWriter writer = new FileWriter(outputPath)) {
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
            store.dispose();
            return true;
        } catch (Exception e) {
            store.dispose();
            throw new IOException("GeoJSON转换失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证SHP文件的完整性
     * 检查是否包含必需的文件组件
     * 
     * @param shpFiles SHP相关文件列表
     * @return 验证结果信息
     */
    public static Map<String, Object> validateShapefiles(List<String> shpFiles) {
        Map<String, Object> result = new HashMap<>();
        
        boolean hasShp = shpFiles.stream().anyMatch(f -> f.toLowerCase().endsWith(".shp"));
        boolean hasShx = shpFiles.stream().anyMatch(f -> f.toLowerCase().endsWith(".shx"));
        boolean hasDbf = shpFiles.stream().anyMatch(f -> f.toLowerCase().endsWith(".dbf"));
        boolean hasPrj = shpFiles.stream().anyMatch(f -> f.toLowerCase().endsWith(".prj"));
        
        List<String> missingFiles = new ArrayList<>();
        if (!hasShp) missingFiles.add(".shp");
        if (!hasShx) missingFiles.add(".shx");
        if (!hasDbf) missingFiles.add(".dbf");
        
        result.put("isValid", missingFiles.isEmpty());
        result.put("missingFiles", missingFiles);
        result.put("hasProjection", hasPrj);
        result.put("foundFiles", shpFiles);
        
        return result;
    }

    /**
     * 从文件列表中找到主SHP文件
     * 
     * @param files 文件路径列表
     * @return SHP文件路径，如果找不到则返回null
     */
    public static String findMainShpFile(List<String> files) {
        return files.stream()
                .filter(f -> f.toLowerCase().endsWith(".shp"))
                .findFirst()
                .orElse(null);
    }
}
