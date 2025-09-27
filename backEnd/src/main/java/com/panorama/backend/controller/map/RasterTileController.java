package com.panorama.backend.controller.map;

import com.panorama.backend.DTO.InfoDTO;
import com.panorama.backend.model.node.LayerNode;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.service.map.RasterTileService;
import com.panorama.backend.service.node.LayerNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.ByteBuffer;

/**
 * @author: DMK
 * @description:
 * @date: 2024-09-18 09:39:02
 * @version: 1.0
 */
@RestController
@RequestMapping("api/v0/resource/raster")
public class RasterTileController {

    private RasterTileService rasterTileService;
    private LayerNodeService layerNodeService;

    @Autowired
    public void setRasterTileService(RasterTileService rasterTileService, LayerNodeService layerNodeService) {
        this.rasterTileService = rasterTileService;
        this.layerNodeService = layerNodeService;
    }

    @GetMapping("/getRasterTile/{id}/{z}/{x}/{y}")
    public ResponseEntity<?> getRasterTile(
            @PathVariable String id,
            @PathVariable int z, @PathVariable int x, @PathVariable int y) throws Exception {

        LayerNode layerNode = layerNodeService.getLayerNodeById(id);

        String type = layerNode.getUsage().get("type");

        switch (type) {
            case "land", "water", "land_gdal" -> {
                FileSystemResource fileSystemResource = rasterTileService.getRasterTile(layerNode, z, x, y);

                HttpHeaders headers = new HttpHeaders();
                if (fileSystemResource != null) {
                    headers.add("Content-Disposition", "inline; filename=" + fileSystemResource.getFilename());
                    headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
                    return ResponseEntity.ok().header(String.valueOf(headers)).contentType(MediaType.IMAGE_PNG).body(fileSystemResource);
                } else {
                    return ResponseEntity.noContent().build();
                }
            }
            case "bundle" -> {
                byte[] imageBytes = rasterTileService.getRasterTileInBundle(layerNode, z, x, y);
                // 设置响应头和内容类型
                if (imageBytes.length > 0) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.IMAGE_PNG);
                    headers.setContentLength(imageBytes.length);
                    return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
                } else {
                    return ResponseEntity.noContent().build();
                }
            }
            case "base" -> {
                ByteBuffer tileData = rasterTileService.getRasterDBTile(layerNode, z, x, y);
                return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "image/png").body(tileData.array());
            }
            default -> {
                return ResponseEntity.noContent().build();
            }
        }
    }

    @PutMapping("/update")
    public ResponseEntity<GeneralResult> updateRasterLayer(@RequestBody InfoDTO info) {
        LayerNode layerNode = layerNodeService.getLayerNodeById(info.getId());
        GeneralResult result = rasterTileService.updateRasterLayer(layerNode, info);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<GeneralResult> deleteRasterLayer(@PathVariable String id) {
        LayerNode layerNode = layerNodeService.getLayerNodeById(id);
        GeneralResult result = rasterTileService.deleteRasterLayer(layerNode);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    @PostMapping("/upload")
    public ResponseEntity<GeneralResult> uploadRasterLayer(@RequestPart("file") MultipartFile file, @RequestPart("info") InfoDTO info) {
        LayerNode parentNode = layerNodeService.getLayerNodeById(info.getParent_id());
        GeneralResult result = rasterTileService.uploadRasterLayer(parentNode, file, info);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

}
