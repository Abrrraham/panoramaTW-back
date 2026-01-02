package com.panorama.backend.controller.admin;

import com.panorama.backend.DTO.admin.LayerListResponse;
import com.panorama.backend.DTO.admin.LayerUpdateRequest;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.service.admin.AdminLayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v0/admin/layers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminLayerController {

    private final AdminLayerService adminLayerService;

    @GetMapping
    public ResponseEntity<GeneralResult> listLayers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        LayerListResponse data = adminLayerService.listLayers(page, size, category, status, keyword);
        return ResponseEntity.ok(GeneralResult.builder()
                .status("success")
                .data(data)
                .build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<GeneralResult> updateLayer(@PathVariable String id, @RequestBody LayerUpdateRequest request) {
        return ResponseEntity.ok(adminLayerService.updateLayer(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GeneralResult> deleteLayer(@PathVariable String id) {
        return ResponseEntity.ok(adminLayerService.deleteLayer(id));
    }
}
