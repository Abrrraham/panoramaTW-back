package com.panorama.backend.controller.admin;

import com.panorama.backend.DTO.InfoDTO;
import com.panorama.backend.model.node.LayerNode;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.service.map.ThreeDTileService;
import com.panorama.backend.service.node.LayerNodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/v0/admin/3d")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ThreeDLayerAdminController {

    private final ThreeDTileService threeDTileService;
    private final LayerNodeService layerNodeService;

    @PostMapping("/layer")
    public ResponseEntity<GeneralResult> upload(@RequestPart("file") MultipartFile file,
                                                @RequestPart("info") InfoDTO info) {
        LayerNode parentNode = layerNodeService.getLayerNodeById(info.getParent_id());
        return ResponseEntity.ok(threeDTileService.upload3DTileLayer(parentNode, file, info));
    }

    @DeleteMapping("/layer/{id}")
    public ResponseEntity<GeneralResult> delete(@PathVariable String id) {
        LayerNode node = layerNodeService.getLayerNodeById(id);
        return ResponseEntity.ok(threeDTileService.delete3DTileLayer(node));
    }
}
