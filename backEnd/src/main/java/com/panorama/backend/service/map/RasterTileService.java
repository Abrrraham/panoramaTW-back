package com.panorama.backend.service.map;

import com.panorama.backend.DTO.InfoDTO;
import com.panorama.backend.annotation.DynamicNodeData;
import com.panorama.backend.mapper.RasterTileMapper;
import com.panorama.backend.model.Constant.GenerateResultStatus;
import com.panorama.backend.model.Constant.TaskStatus;
import com.panorama.backend.model.Constant.TaskType;
import com.panorama.backend.model.node.LayerNode;
import com.panorama.backend.model.node.ModelNode;
import com.panorama.backend.model.node.TaskNode;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.model.resource.Tile;
import com.panorama.backend.service.node.LayerNodeService;
import com.panorama.backend.service.node.ModelNodeService;
import com.panorama.backend.service.node.TaskNodeService;
import com.panorama.backend.service.resource.AsyncTaskService;
import com.panorama.backend.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: DMK
 * @description:
 * @date: 2024-09-17 21:48:02
 * @version: 1.0
 */
@Service
@Slf4j
public class RasterTileService {

    @Value("${path.temp}")
    private String temp;

    @Value("${path.rasterTile}")
    private String rasterTilePath;

    @Value("${path.rasterFile}")
    private String rasterFilePath;

    private LayerNodeService layerNodeService;
    private TaskNodeService taskNodeService;
    private ModelNodeService modelNodeService;
    private AsyncTaskService asyncTaskService;
    private RasterTileMapper rasterTileMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final int TILE_SIZE = 256;
    private static final double DEG_TO_RAD = Math.PI / 180.0;
    private static final double RAD_TO_DEG = 180.0 / Math.PI;

    @Autowired
    public void setRasterTileService(LayerNodeService layerNodeService, AsyncTaskService asyncTaskService, ModelNodeService modelNodeService, TaskNodeService taskNodeService, RasterTileMapper rasterTileMapper) {
        this.layerNodeService = layerNodeService;
        this.modelNodeService = modelNodeService;
        this.taskNodeService = taskNodeService;
        this.asyncTaskService = asyncTaskService;
        this.rasterTileMapper = rasterTileMapper;
    }

    public FileSystemResource getRasterTile(LayerNode layerNode, int z, int x, int y) {

        String url = layerNode.getDataSource().get("url");
        String specific_rasterTile_path = "";
        //数据库文件型栅格图层
        if (layerNode.getDataSource().containsKey("username")){
            return null;
        }else {
            String type = layerNode.getUsage().get("type");
            if (type.equals("land_gdal")){
                int y_new = (int) (Math.pow(2, z) - 1.0 - y);
                specific_rasterTile_path = String.join(File.separator, url, layerNode.getTableName(), String.valueOf(z), String.valueOf(x), y_new + ".png");
            }else if (type.equals("land") || type.equals("water")){
                specific_rasterTile_path = String.join(File.separator, url, layerNode.getTableName(), String.valueOf(z), x + "-" + y + "-" + z + ".png");
            }
            File rasterTile = new File(specific_rasterTile_path);
            // 检查文件是否存在
            if (!rasterTile.exists()) {
                return null;
            }
            return new FileSystemResource(rasterTile);
        }
    }

    @DynamicNodeData
    public ByteBuffer getRasterDBTile(LayerNode layerNode, int z, int x, int y) {
        int y_new = (int) (Math.pow(2, z) - 1.0 - y);
        Tile tileData = rasterTileMapper.getRasterDBTile(z, x, y_new);
        if(tileData != null){
            return ByteBuffer.wrap(tileData.getTile_data());
        }else {
            return ByteBuffer.wrap("null data".getBytes());
        }
    }

    @DynamicNodeData
    public GeneralResult updateRasterLayer(LayerNode layerNode, InfoDTO infoDTO){
        if (layerNodeService.updateLayer(layerNode, infoDTO)){
            return GeneralResult.builder().status(GenerateResultStatus.SUCCESS).message("update raster layer successfully").build();
        }else{
            return GeneralResult.builder().status(GenerateResultStatus.ERROR).message("failed to update raster layer").build();
        }
    }

    public GeneralResult deleteRasterLayer(LayerNode layerNode){

        String url = layerNode.getDataSource().get("url");
        TaskNode taskNode = TaskNode.builder().status(TaskStatus.NONE).layerNode(layerNode).type(TaskType.DELETE).build();
        ModelNode modelNode;
        String taskNodeId = taskNodeService.saveTaskNode(taskNode);
        Map<String, String> params = new HashMap<>();

        try {
            //数据库文件型栅格图层
            if (layerNode.getDataSource().containsKey("username")){
                String dbFilePath = url.split(":")[2];
                params.put("path", dbFilePath);
                modelNode = modelNodeService.getModelNodeByName("deleteFile");

            }else{
                String tableName = layerNode.getTableName();
                String rasterPath = String.join(File.separator, url, tableName);
                params.put("path", rasterPath);
                modelNode = modelNodeService.getModelNodeByName("deleteDirectory");
            }
            taskNode.setParams(params);
            taskNode.setModelNode(modelNode);
            taskNodeService.saveTaskNode(taskNode);
            asyncTaskService.systemTaskAsync(taskNodeId);
            return GeneralResult.builder().status(GenerateResultStatus.RUNNING).message(taskNodeId).build();
        } catch (Exception e) {
            return GeneralResult.builder().status(GenerateResultStatus.ERROR).message("failed to delete raster layer").build();
        }
    }

    public GeneralResult uploadRasterLayer(LayerNode parentNode, MultipartFile multipartFile, InfoDTO infoDTO) {
        try {
            //M2F
            File zipFile = FileUtil.convertMultipartFileToFile(multipartFile, temp);
            String path = zipFile.getParent();
            //解压
            List<String> list = FileUtil.unZipFiles(zipFile, path);
            boolean result = zipFile.delete();
            if (!result){
                log.error("failed to delete zip file");
            }
            //找到tif文件
            String tifPath = FileUtil.findFileWithExtension(list, ".tif");
            if (tifPath.isEmpty()){
                return GeneralResult.builder().status("error").message("tif not found").build();
            }else {
                ModelNode modelNode = modelNodeService.getModelNodeByName("tif2Tile");

                Map<String, String> params = new HashMap<>();
                String minZoom = infoDTO.getUsage().get("minZoom");
                String maxZoom = infoDTO.getUsage().get("maxZoom");

                String tableName = infoDTO.getTableName();
                long sameCount = FileUtil.countFilesWithPrefix(rasterTilePath, tableName);
                String uniqueTableName;
                if (sameCount == 0){
                    uniqueTableName = tableName;
                }else {
                    uniqueTableName = tableName + "_" + sameCount;
                }

                String outputPath = String.join(File.separator, rasterTilePath, uniqueTableName);
                params.put("zoom", minZoom + "-" + maxZoom);
                params.put("tifPath", tifPath);
                params.put("outputPath", outputPath);
                params.put("size", infoDTO.getUsage().get("size"));

                Map<String, String> dataSourceMap = new HashMap<>();
                dataSourceMap.put("url", rasterTilePath);
                //创建LayerNode但暂不保存
                LayerNode layerNode = LayerNode.builder()
                        .tableName(uniqueTableName).layerName(infoDTO.getLayerName())
                        .category("raster").usage(infoDTO.getUsage())
                        .path(layerNodeService.getNodePath(parentNode)).dataSource(dataSourceMap)
                        .build();

                TaskNode taskNode = TaskNode.builder().status(TaskStatus.NONE).params(params).modelNode(modelNode)
                        .layerNode(layerNode).tempPath(path).type(TaskType.UPLOAD).build();
                String taskNodeId = taskNodeService.saveTaskNode(taskNode);

                asyncTaskService.modelTaskAsync(taskNodeId);

                return GeneralResult.builder().status(GenerateResultStatus.RUNNING).message(taskNodeId).build();
            }
        } catch (Exception e) {
            return GeneralResult.builder().status(GenerateResultStatus.ERROR).message("failed to upload raster layer").build();
        }
    }

    // Step 1: 计算瓦片的经纬度范围 (EPSG:4490)
    public static double[] tileToLatLon4490(int z, int x, int y) {
        double resolution = 360.0 / Math.pow(2, z); // 每瓦片的经纬度分辨率
        double minLon = x * resolution - 180;
        double maxLon = (x + 1) * resolution - 180;
        double minLat = 90 - (y + 1) * resolution;
        double maxLat = 90 - y * resolution;
        return new double[]{minLon, maxLon, minLat, maxLat};
    }

    // Step 2: 经纬度 (EPSG:4490) 转换为 EPSG:3857
    public static double[] latLonToMercator(double lat, double lon) {
        try {
            // 获取 EPSG:4490 和 EPSG:3857 的转换器
            CoordinateReferenceSystem srcCRS = CRS.decode("EPSG:4490");
            CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:3857");

            MathTransform transform = CRS.findMathTransform(srcCRS, targetCRS, true);

            DirectPosition2D src = new DirectPosition2D(lon, lat);  // 使用 DirectPosition2D
            DirectPosition2D dest = new DirectPosition2D();
            transform.transform(src, dest);

            return new double[]{dest.x, dest.y};
        } catch (Exception e) {
            e.printStackTrace();
            return new double[0];
        }
    }

    // Step 3: 将 EPSG:3857 坐标转换为瓦片编号
    public static int[] mercatorToTile3857(int z, double mx, double my) {
        double earthCircumference = 2 * Math.PI * 6378137; // 地球周长
        double originShift = earthCircumference / 2.0;
        double resolution = earthCircumference / (TILE_SIZE * Math.pow(2, z));

        int tileX = (int) Math.floor((mx + originShift) / (resolution * TILE_SIZE));
        int tileY = (int) Math.floor((originShift - my) / (resolution * TILE_SIZE));
        return new int[]{tileX, tileY};
    }

    // 从 EPSG:3857 坐标转换为经纬度
    public static double[] mercatorToLatLon(double my, double mx) {
        try {
            // 获取 EPSG:3857 和 EPSG:4490 的转换器
            CoordinateReferenceSystem srcCRS = CRS.decode("EPSG:4326");
            CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4490");

            MathTransform transform = CRS.findMathTransform(srcCRS, targetCRS, true);

            DirectPosition2D src = new DirectPosition2D(mx, my); // 使用 DirectPosition2D
            DirectPosition2D dest = new DirectPosition2D();
            transform.transform(src, dest);

            return new double[]{dest.getY(), dest.getX()};
        } catch (Exception e) {
            e.printStackTrace();
            return new double[0];
        }
    }

    public static double tile2lon(int x, int z) {
        return x / Math.pow(2.0, z) * 360.0 - 180.0;
    }

    public static double tile2lat(int y, int z) {
        double n = Math.PI - 2.0 * Math.PI * y / Math.pow(2.0, z);
        return RAD_TO_DEG * Math.atan(0.5 * (Math.exp(n) - Math.exp(-n)));
    }

    public static double[] tileToBBox(int[] tile) {
        double w = tile2lon(tile[0], tile[2]);
        double e = tile2lon(tile[0] + 1, tile[2]);
        double n = tile2lat(tile[1], tile[2]);
        double s = tile2lat(tile[1] + 1, tile[2]);
        return new double[] {w, s, e, n};
    }

    // Step 4: 逆向转换：3857 z,x,y -> 4490 z,x,y
    public static double[] convertTile3857To4490(int z, int x, int y) {
        // 1. 从瓦片编号计算中心点的 EPSG:3857 坐标
//        double resolution = 360.0 / Math.pow(2, z);
//        double mx = x * TILE_SIZE * resolution - 180;
//        double my = 180 - (y * TILE_SIZE * resolution);
        int[] tile = new int[3];
        tile[0] = x;
        tile[1] = y;
        tile[2] = z;
        double[] bbox = tileToBBox(tile);
        double[] center = new double[2];
        center[0] = (bbox[0] + bbox[2]) / 2.0;
        center[1] = (bbox[1] + bbox[3]) / 2.0;

        // 2. EPSG:3857 转换为 EPSG:4490 经纬度
//        double[] latLon = mercatorToLatLon(center[1], center[0]);
//        double lat = latLon[0];
//        double lon = latLon[1];

        // 3. 计算对应的 EPSG:4490 瓦片编号
//        int tileX = (int) ((lon + 180) / resolution);
//        int tileY = (int) ((90 - lat) / resolution);
//        int tileX = (int) Math.floor((lon + 180.0) / (360.0 / Math.pow(2.0,z)));
//        int tileY = (int) Math.floor((90.0 - lat) / (180.0 / Math.pow(2.0,z)));
        return new double[]{bbox[0], bbox[3]};
    }

    public byte[] getRasterTileInBundle(LayerNode layerNode, int z, int x, int y) {

//        double[] tile4490 = convertTile3857To4490(z, x, y);
//        int newX = tile4490[0];
//        int newY = tile4490[1];

        String url = layerNode.getUsage().get("url");
        // 创建请求体对象
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("layerPath", layerNode.getDataSource().get("url"));
        requestBody.put("z", String.valueOf(z));
        requestBody.put("x", String.valueOf(x));
        requestBody.put("y", String.valueOf(y));

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 封装请求头和请求体
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        byte[] tileBytes;
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                byte[].class
            );

            // 获取 PNG 二进制流
            tileBytes = response.getBody();
        } catch (RestClientException e) {
            throw new RuntimeException(e);
        }

        return tileBytes;
    }
}
