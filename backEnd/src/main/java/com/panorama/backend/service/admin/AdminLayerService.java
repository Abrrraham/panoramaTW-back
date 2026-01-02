package com.panorama.backend.service.admin;

import com.panorama.backend.DTO.admin.LayerListResponse;
import com.panorama.backend.DTO.admin.LayerSummaryDTO;
import com.panorama.backend.DTO.admin.LayerUpdateRequest;
import com.panorama.backend.model.Constant.LayerStatus;
import com.panorama.backend.model.node.LayerNode;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.repository.LayerNodeRepo;
import com.panorama.backend.service.map.RasterTileService;
import com.panorama.backend.service.map.VectorTileService;
import com.panorama.backend.service.node.LayerNodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminLayerService {

    private final LayerNodeRepo layerNodeRepo;
    private final LayerNodeService layerNodeService;
    private final VectorTileService vectorTileService;
    private final RasterTileService rasterTileService;

    public LayerListResponse listLayers(int page, int size, String category, String status, String keyword) {
        List<LayerNode> all = layerNodeRepo.findAll();
        String categoryFilter = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
        String statusFilter = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        String keywordFilter = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        List<LayerSummaryDTO> items = all.stream()
                .filter(node -> node.getUsage() != null)
                .filter(node -> categoryFilter.isEmpty()
                        || (node.getCategory() != null
                        && node.getCategory().toLowerCase(Locale.ROOT).equals(categoryFilter)))
                .map(this::normalizeUsageForList)
                .filter(node -> statusFilter.isEmpty()
                        || statusFilter.equals(node.getUsage().getOrDefault("status", LayerStatus.READY)))
                .filter(node -> keywordFilter.isEmpty()
                        || (node.getLayerName() != null && node.getLayerName().toLowerCase(Locale.ROOT).contains(keywordFilter))
                        || (node.getTableName() != null && node.getTableName().toLowerCase(Locale.ROOT).contains(keywordFilter)))
                .sorted(Comparator.comparing(LayerNode::getCreatedAt, Comparator.nullsLast(Long::compareTo)).reversed())
                .map(node -> LayerSummaryDTO.builder()
                        .id(node.getId())
                        .layerName(node.getLayerName())
                        .tableName(node.getTableName())
                        .category(node.getCategory())
                        .usage(node.getUsage())
                        .createdAt(node.getCreatedAt())
                        .updatedAt(node.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        int total = items.size();
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(total, from + size);
        List<LayerSummaryDTO> pageItems = from >= total ? new ArrayList<>() : items.subList(from, to);

        return LayerListResponse.builder()
                .items(pageItems)
                .page(page)
                .size(size)
                .total(total)
                .build();
    }

    public GeneralResult updateLayer(String id, LayerUpdateRequest request) {
        LayerNode layerNode = layerNodeService.getLayerNodeById(id);
        if (layerNode == null) {
            return GeneralResult.builder()
                    .code("LAYER_NOT_FOUND")
                    .status("error")
                    .message("layer not found")
                    .build();
        }
        boolean updated = false;
        if (request.getLayerName() != null && !request.getLayerName().isBlank()) {
            layerNode.setLayerName(request.getLayerName().trim());
            updated = true;
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            String nextStatus = request.getStatus().trim().toUpperCase(Locale.ROOT);
            if (!LayerStatus.READY.equals(nextStatus) && !LayerStatus.DISABLED.equals(nextStatus)) {
                return GeneralResult.builder()
                        .code("INVALID_STATUS")
                        .status("error")
                        .message("status must be READY or DISABLED")
                        .build();
            }
            Map<String, String> usage = layerNode.getUsage() == null ? new HashMap<>() : new HashMap<>(layerNode.getUsage());
            usage.put("status", nextStatus);
            layerNode.setUsage(usage);
            updated = true;
        }

        if (!updated) {
            return GeneralResult.builder()
                    .code("NO_CHANGE")
                    .status("error")
                    .message("no update fields provided")
                    .build();
        }

        layerNode.setUpdatedAt(System.currentTimeMillis());
        layerNodeService.saveLayerNode(layerNode);
        return GeneralResult.builder()
                .status("success")
                .message("layer updated")
                .data(layerNode.getId())
                .build();
    }

    public GeneralResult deleteLayer(String id) {
        LayerNode layerNode = layerNodeService.getLayerNodeById(id);
        if (layerNode == null) {
            return GeneralResult.builder()
                    .code("LAYER_NOT_FOUND")
                    .status("error")
                    .message("layer not found")
                    .build();
        }
        String category = layerNode.getCategory() == null ? "" : layerNode.getCategory().toLowerCase(Locale.ROOT);
        if ("vector".equals(category)) {
            return vectorTileService.deleteVectorLayer(layerNode);
        }
        if ("raster".equals(category)) {
            return rasterTileService.deleteRasterLayerSync(layerNode);
        }
        return GeneralResult.builder()
                .code("UNSUPPORTED_CATEGORY")
                .status("error")
                .message("unsupported layer category")
                .build();
    }

    private LayerNode normalizeUsageForList(LayerNode node) {
        if (node.getUsage() == null) {
            return node;
        }
        Map<String, String> usage = new HashMap<>(node.getUsage());
        String current = usage.get("status");
        if (current == null || current.isBlank()) {
            usage.put("status", LayerStatus.READY);
        } else {
            usage.put("status", current.toUpperCase(Locale.ROOT));
        }
        node.setUsage(usage);
        return node;
    }
}
