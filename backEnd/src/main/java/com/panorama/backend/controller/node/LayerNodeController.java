package com.panorama.backend.controller.node;

import com.panorama.backend.DTO.InfoDTO;
import com.panorama.backend.DTO.LayerNodeDTO;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.service.node.LayerNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * @author: DMK
 * @description:
 * @date: 2024-09-14 15:18:33
 * @version: 1.0
 */
@RestController
@RequestMapping("api/v0/node/layerNode")
public class LayerNodeController {

    private LayerNodeService layerNodeService;

    @Autowired
    public void LayerNodeService(LayerNodeService layerNodeService) {
        this.layerNodeService = layerNodeService;
    }

    @GetMapping("/getLayerTree")
    public ResponseEntity<LayerNodeDTO> getAllInfo(){
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .body(layerNodeService.getLayerTree());
    }

    @GetMapping("/test")
    public ResponseEntity<String> test(){
        return ResponseEntity.ok("API is working!");
    }

    @PostMapping("/createCategory")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GeneralResult> createCategory(@RequestBody InfoDTO info) throws IOException {
        GeneralResult result = layerNodeService.createCategory(info);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

//    @DeleteMapping("/deleteCategory/{id}")
//    public ResponseEntity<String> deleteCategory(@PathVariable String id) throws JsonProcessingException {
//        LayerNode layerNode = layerNodeService.getLayerNodeById(id);
//        GeneralResult result = layerNodeService.deleteCategory(layerNode);
//        HttpHeaders headers = new HttpHeaders();
//        return ResponseEntity.ok().headers(headers)
//                .body(JsonUtil.serializeObject(result));
//    }

    @PutMapping("/updateCategory")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GeneralResult> updateCategory(@RequestBody InfoDTO info) throws IOException {
        GeneralResult result = layerNodeService.updateCategory(info);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

}
