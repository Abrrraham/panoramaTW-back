package com.panorama.backend.controller.map;

import com.panorama.backend.DTO.InfoDTO;
import com.panorama.backend.model.node.LayerNode;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.service.map.ThreeDTileService;
import com.panorama.backend.service.node.LayerNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author: DMK
 * @description:
 * @date: 2024-09-28 22:32:15
 * @version: 1.0
 */
@RestController
@RequestMapping("api/")
public class ThreeDTileController {

    private LayerNodeService layerNodeService;
    private ThreeDTileService threeDTileService;

    @Autowired
    public void setThreeDTileController(LayerNodeService layerNodeService, ThreeDTileService threeDTileService) {
        this.layerNodeService = layerNodeService;
        this.threeDTileService = threeDTileService;
    }

    // 提供初始的 tileset.json
    @GetMapping("/get3DTiles/{id}/{filename}")
    public ResponseEntity<Resource> get3DTiles(@PathVariable String id, @PathVariable String filename) {
        LayerNode layerNode = layerNodeService.getLayerNodeById(id);
        String url = layerNode.getDataSource().get("url");
        String path = String.join(File.separator, url, layerNode.getTableName(), filename);
        Path filePath = Paths.get(path);
        return serveFile(filePath);
    }

    // 提供多层级的 tileset.json 和 b3dm 文件
    @GetMapping("/get3DTiles/{id}/{z}/{filename}")
    public ResponseEntity<Resource> get3DTiles(@PathVariable String id, @PathVariable String z, @PathVariable String filename) {
        LayerNode layerNode = layerNodeService.getLayerNodeById(id);
        String url = layerNode.getDataSource().get("url");
        String path = String.join(File.separator, url, layerNode.getTableName(), z, filename);
        Path filePath = Paths.get(path);
        return serveFile(filePath);
    }

    // 统一处理文件请求的方法
    private ResponseEntity<Resource> serveFile(Path filePath) {
        Resource resource = new FileSystemResource(filePath.toFile());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        // 根据文件类型返回不同的 MediaType
        String fileName = filePath.getFileName().toString();
        MediaType mediaType = getMediaTypeForFileName(fileName);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }

    // 判断文件类型返回适当的 MediaType
    private MediaType getMediaTypeForFileName(String fileName) {
        if (fileName.endsWith(".json")) {
            return MediaType.APPLICATION_JSON;
        } else if (fileName.endsWith(".b3dm")) {
            return MediaType.APPLICATION_OCTET_STREAM;
        } else {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @PutMapping("/update")
    public ResponseEntity<GeneralResult> update3DLayer(@RequestBody InfoDTO info) {
        LayerNode layerNode = layerNodeService.getLayerNodeById(info.getId());
        GeneralResult result = threeDTileService.update3DTileLayer(layerNode, info);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<GeneralResult> delete3DLayer(@PathVariable String id) {
        LayerNode layerNode = layerNodeService.getLayerNodeById(id);
        GeneralResult result = threeDTileService.delete3DTileLayer(layerNode);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    @PostMapping("/upload")
    public ResponseEntity<GeneralResult> upload3DLayer(@RequestPart("file") MultipartFile file, @RequestPart("info") InfoDTO info) {
        LayerNode parentNode = layerNodeService.getLayerNodeById(info.getParent_id());
        GeneralResult result = threeDTileService.upload3DTileLayer(parentNode, file, info);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }


}
