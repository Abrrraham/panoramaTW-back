package com.panorama.backend.controller.resource;

import com.panorama.backend.DTO.InfoDTO;
import com.panorama.backend.model.node.LayerNode;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.service.node.LayerNodeService;
import com.panorama.backend.service.resource.StaticFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author: DMK
 * @description:
 * @date: 2024-09-27 17:31:18
 * @version: 1.0
 */
@RestController
@RequestMapping("api/v0/resource/static")
public class StaticFileController {

    private StaticFileService staticFileService;
    private LayerNodeService layerNodeService;

    @Autowired
    public void setStaticFileController(StaticFileService staticFileService, LayerNodeService layerNodeService) {
        this.staticFileService = staticFileService;
        this.layerNodeService = layerNodeService;
    }

    @GetMapping("/getStaticFileByte/{id}")
    public ResponseEntity<byte[]> getStaticFileByte(@PathVariable String id) throws IOException {

        LayerNode layerNode = layerNodeService.getLayerNodeById(id);
        byte[] staticFileByte = staticFileService.getStaticFileByte(layerNode);

        HttpHeaders headers = new HttpHeaders();
        String suffix = layerNode.getUsage().get("type");
        switch (suffix) {
            case "pdf" -> headers.setContentType(MediaType.APPLICATION_PDF);
            case "json" -> headers.setContentType(MediaType.APPLICATION_JSON);
            case "txt" -> headers.setContentType(MediaType.TEXT_PLAIN);
        }

        headers.setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic());
        headers.setContentDispositionFormData("inline", layerNode.getLayerName());
        return ResponseEntity.ok().header(String.valueOf(headers)).contentType(MediaType.APPLICATION_PDF).body(staticFileByte);

    }

    @PutMapping("/update")
    public ResponseEntity<GeneralResult> updateStaticLayer(@RequestBody InfoDTO info) {
        LayerNode layerNode = layerNodeService.getLayerNodeById(info.getId());
        GeneralResult result = staticFileService.updateStaticLayer(layerNode, info);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<GeneralResult> deleteStaticLayer(@PathVariable String id) {
        LayerNode layerNode = layerNodeService.getLayerNodeById(id);
        GeneralResult result = staticFileService.deleteStaticLayer(layerNode);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    @PostMapping("/upload")
    public ResponseEntity<GeneralResult> uploadRasterLayer(@RequestPart("file") MultipartFile file, @RequestPart("info") InfoDTO info) {
        LayerNode parentNode = layerNodeService.getLayerNodeById(info.getParent_id());
        GeneralResult result = staticFileService.uploadStaticLayer(parentNode, file, info);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

}
