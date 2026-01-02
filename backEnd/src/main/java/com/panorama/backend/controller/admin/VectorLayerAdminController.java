package com.panorama.backend.controller.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.panorama.backend.DTO.InfoDTO;
import com.panorama.backend.model.node.LayerNode;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.service.map.VectorTileService;
import com.panorama.backend.service.node.LayerNodeService;
import lombok.RequiredArgsConstructor;
import org.opengis.referencing.FactoryException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("api/v0/admin/vector")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class VectorLayerAdminController {

    private final VectorTileService vectorTileService;
    private final LayerNodeService layerNodeService;

    @PostMapping("/layer")
    public ResponseEntity<GeneralResult> uploadVectorLayer(@RequestPart("file") MultipartFile file,
                                                           @RequestPart("info") InfoDTO info) throws IOException {
        LayerNode parentNode = layerNodeService.getLayerNodeById(info.getParent_id());
        if (parentNode == null) {
            return ResponseEntity.ok(GeneralResult.builder()
                    .code("PARENT_NOT_FOUND")
                    .status("error")
                    .message("parent node not found")
                    .build());
        }
        return ResponseEntity.ok(vectorTileService.uploadJSONLayer(parentNode, file, info));
    }

    @PostMapping("/layer/shp")
    public ResponseEntity<GeneralResult> uploadShape(@RequestPart("file") MultipartFile file,
                                                     @RequestPart("info") InfoDTO info) throws IOException, InterruptedException, FactoryException {
        LayerNode parentNode = layerNodeService.getLayerNodeById(info.getParent_id());
        if (parentNode == null) {
            return ResponseEntity.ok(GeneralResult.builder()
                    .code("PARENT_NOT_FOUND")
                    .status("error")
                    .message("parent node not found")
                    .build());
        }
        GeneralResult parseResult = vectorTileService.parseShpLayer(file);
        if (!"success".equalsIgnoreCase(parseResult.getStatus())) {
            return ResponseEntity.ok(parseResult);
        }
        Object message = parseResult.getMessage();
        String path = null;
        if (message instanceof String) {
            path = (String) message;
        } else if (message instanceof java.util.Map<?, ?> msgMap) {
            Object pathObj = msgMap.get("path");
            if (pathObj != null) {
                path = pathObj.toString();
            }
        }
        if (path == null || path.isEmpty()) {
            return ResponseEntity.ok(GeneralResult.builder().status("error").message("failed to parse shp path").build());
        }
        return ResponseEntity.ok(vectorTileService.storeShpLayer(parentNode, path, info));
    }

    @DeleteMapping("/layer/{id}")
    public ResponseEntity<GeneralResult> deleteLayer(@PathVariable String id) throws JsonProcessingException {
        LayerNode layerNode = layerNodeService.getLayerNodeById(id);
        return ResponseEntity.ok(vectorTileService.deleteVectorLayer(layerNode));
    }
}
