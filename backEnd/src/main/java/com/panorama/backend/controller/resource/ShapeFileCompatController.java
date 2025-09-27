package com.panorama.backend.controller.resource;

import com.panorama.backend.DTO.InfoDTO;
import com.panorama.backend.model.Constant.GenerateResultStatus;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.service.resource.ShapeFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Map;

/**
 * SHP文件兼容性控制器
 * 提供向后兼容的API接口
 * 
 * @author: Panorama System
 * @description: 兼容旧版本的SHP文件上传接口
 * @date: 2024-09-27
 * @version: 1.0
 */
@Slf4j
@RestController
@RequestMapping("api/resource/shp")
@CrossOrigin(origins = "*")
public class ShapeFileCompatController {

    private ShapeFileService shapeFileService;

    @Autowired
    public void setShapeFileService(ShapeFileService shapeFileService) {
        this.shapeFileService = shapeFileService;
    }

    /**
     * 健康检查接口
     * 
     * @return 服务状态
     */
    @GetMapping("/health")
    public ResponseEntity<GeneralResult> health() {
        log.info("SHP上传服务健康检查");
        GeneralResult result = GeneralResult.builder()
                .status(GenerateResultStatus.SUCCESS)
                .message("SHP文件上传服务运行正常")
                .data(Map.of(
                        "service", "ShapeFileService",
                        "timestamp", System.currentTimeMillis(),
                        "supportedFormats", Arrays.asList("zip", "shp", "geojson")
                ))
                .build();
        
        return ResponseEntity.ok(result);
    }

    /**
     * 兼容性上传接口
     * 支持旧版本的上传路径: /api/resource/shp/upload
     * 
     * @param file 上传的文件
     * @return 上传结果
     */
    @PostMapping("/upload")
    public ResponseEntity<GeneralResult> uploadShapeFile(@RequestParam("file") MultipartFile file) {
        log.info("使用兼容性接口上传文件: {}", file.getOriginalFilename());
        
        // 创建默认的图层信息
        InfoDTO defaultInfo = new InfoDTO();
        defaultInfo.setLayerName(getDefaultLayerName(file.getOriginalFilename()));
        defaultInfo.setTableName(getDefaultTableName(file.getOriginalFilename()));
        defaultInfo.setParent_id("root"); // 默认父节点
        
        GeneralResult result = shapeFileService.uploadShapeFile(file, defaultInfo);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    /**
     * 从文件名生成默认图层名称
     */
    private String getDefaultLayerName(String filename) {
        if (filename == null) return "未命名图层";
        String name = filename;
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            name = name.substring(0, dotIndex);
        }
        return name.replaceAll("[^a-zA-Z0-9\u4e00-\u9fa5_-]", "_");
    }

    /**
     * 从文件名生成默认表名
     */
    private String getDefaultTableName(String filename) {
        if (filename == null) return "unnamed_layer";
        String name = filename;
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            name = name.substring(0, dotIndex);
        }
        return name.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
    }
}
