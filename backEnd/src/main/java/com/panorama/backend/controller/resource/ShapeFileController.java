package com.panorama.backend.controller.resource;

import com.panorama.backend.DTO.InfoDTO;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.service.resource.ShapeFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * SHP文件导入控制器
 * 支持用户上传SHP文件并自动转换为地图图层
 * 
 * @author: Panorama System
 * @description: 处理SHP文件的上传、解析和导入功能
 * @date: 2024-09-27
 * @version: 1.0
 */
@RestController
@RequestMapping("api/v0/resource/shapefile")
@CrossOrigin(origins = "*")
public class ShapeFileController {

    private ShapeFileService shapeFileService;

    @Autowired
    public void setShapeFileService(ShapeFileService shapeFileService) {
        this.shapeFileService = shapeFileService;
    }

    /**
     * 上传SHP文件
     * 支持压缩包上传，包含.shp, .shx, .dbf, .prj等文件
     * 
     * @param file 上传的文件（通常是包含SHP相关文件的ZIP包）
     * @param info 图层信息，包含名称、描述等
     * @return 上传结果
     */
    @PostMapping("/upload")
    public ResponseEntity<GeneralResult> uploadShapeFile(
            @RequestPart("file") MultipartFile file, 
            @RequestPart("info") InfoDTO info) {
        
        GeneralResult result = shapeFileService.uploadShapeFile(file, info);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    /**
     * 简单上传SHP文件接口
     * 只需要文件参数，使用默认的图层信息
     * 
     * @param file 上传的文件
     * @return 上传结果
     */
    @PostMapping("/upload-simple")
    public ResponseEntity<GeneralResult> uploadShapeFileSimple(@RequestParam("file") MultipartFile file) {
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

    /**
     * 验证SHP文件格式
     * 在上传前检查文件是否包含必需的SHP文件组件
     * 
     * @param file 待验证的文件
     * @return 验证结果
     */
    @PostMapping("/validate")
    public ResponseEntity<GeneralResult> validateShapeFile(@RequestPart("file") MultipartFile file) {
        GeneralResult result = shapeFileService.validateShapeFile(file);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    /**
     * 获取SHP文件预览信息
     * 返回字段信息、要素数量、空间范围等
     * 
     * @param file SHP文件
     * @return 预览信息
     */
    @PostMapping("/preview")
    public ResponseEntity<GeneralResult> previewShapeFile(@RequestPart("file") MultipartFile file) {
        GeneralResult result = shapeFileService.previewShapeFile(file);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    /**
     * 删除已导入的SHP图层
     * 
     * @param id 图层ID
     * @return 删除结果
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<GeneralResult> deleteShapeFileLayer(@PathVariable String id) {
        GeneralResult result = shapeFileService.deleteShapeFileLayer(id);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }
}
