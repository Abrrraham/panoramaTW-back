package com.panorama.backend.service.node;

import com.panorama.backend.DTO.InfoDTO;
import com.panorama.backend.DTO.LayerNodeDTO;
import com.panorama.backend.model.node.LayerNode;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.repository.LayerNodeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author: DMK
 * @description:
 * @date: 2024-09-14 15:14:06
 * @version: 1.0
 */
@Service
public class LayerNodeService {
    private LayerNodeRepo layerNodeRepo;

    @Autowired
    public void setLayerNodeRepo(LayerNodeRepo layerNodeRepo) {
        this.layerNodeRepo = layerNodeRepo;
    }

    public LayerNode getLayerNodeById(String id){
        return layerNodeRepo.findLayerNodeById(id);
    }

    public LayerNode getLayerNodeByTableName(String tableName){
        return layerNodeRepo.findLayerNodeByTableName(tableName);
    }

    public void saveLayerNode(LayerNode layerNode){
        layerNodeRepo.save(layerNode);
    }

    public void deleteLayerNode(LayerNode layerNode){
        layerNodeRepo.delete(layerNode);
    }

    public String getNodePath(LayerNode layerNode){
        String basePath = layerNode.getPath() == null ? "" : layerNode.getPath();
        return basePath + layerNode.getTableName() + ",";
    }

    private List<LayerNode> getChildren(LayerNode layerNode){
        return layerNodeRepo.findLayerNodesByPathStartingWith(getNodePath(layerNode));
    }

    public LayerNodeDTO getLayerTree(){
        LayerNode rootNode = layerNodeRepo.findLayerNodeByTableName("layerNode");
        if (rootNode == null) {
            LayerNode created = LayerNode.builder()
                    .tableName("layerNode")
                    .layerName("layerNode")
                    .category("root")
                    .path("")
                    .createdAt(System.currentTimeMillis())
                    .updatedAt(System.currentTimeMillis())
                    .build();
            saveLayerNode(created);
            rootNode = created;
        }
        return buildTree(rootNode);
    }

    private LayerNodeDTO buildTree(LayerNode layerNode){
        Map<String, String> usage = layerNode.getUsage();
        if (usage != null && !usage.containsKey("status")) {
            usage = new java.util.HashMap<>(usage);
            usage.put("status", com.panorama.backend.model.Constant.LayerStatus.READY);
        }
        LayerNodeDTO layerNodeDTO = LayerNodeDTO.builder()
                .tableName(layerNode.getTableName())
                .layerName(layerNode.getLayerName())
                .category(layerNode.getCategory())
                .usage(usage).id(layerNode.getId())
                .build();
        List<LayerNode> children = layerNodeRepo.findLayerNodesByPath(getNodePath(layerNode));

        for(LayerNode child : children){
            layerNodeDTO.getChildren().add(buildTree(child));
        }

        return layerNodeDTO;
    }

    public GeneralResult createCategory(InfoDTO infoDTO){
        try {
            LayerNode parentNode = layerNodeRepo.findLayerNodeById(infoDTO.getParent_id());
            LayerNode newLayerNode = LayerNode.builder()
                    .tableName(infoDTO.getTableName()).layerName(infoDTO.getLayerName())
                    .path(getNodePath(parentNode))
                    .createdAt(System.currentTimeMillis())
                    .updatedAt(System.currentTimeMillis())
                    .build();
            saveLayerNode(newLayerNode);
            return GeneralResult.builder().status("success").message("create category successfully").build();
        }catch (Exception e){
            return GeneralResult.builder().status("error").message("failed to create category").build();
        }
    }

    public GeneralResult updateCategory(InfoDTO infoDTO){
        try{
            LayerNode layerNode = layerNodeRepo.findLayerNodeById(infoDTO.getId());
            List<LayerNode> children = getChildren(layerNode);

            //重命名
            String oldNodePath = getNodePath(layerNode);
            String newLayerName = infoDTO.getLayerName();
            String newParentId = infoDTO.getParent_id();

            //重命名
            if (newLayerName != null && !newLayerName.isEmpty()){
                layerNode.setLayerName(infoDTO.getLayerName());
            }

            //移动分类
            if (newParentId != null && !newParentId.isEmpty()){
                LayerNode newParentNode = layerNodeRepo.findLayerNodeById(newParentId);
                layerNode.setPath(getNodePath(newParentNode));
            }

            for (LayerNode child : children){
                child.setPath(child.getPath().replace(oldNodePath, getNodePath(layerNode)));
                layerNodeRepo.save(child);
            }

            layerNode.setUpdatedAt(System.currentTimeMillis());
            layerNodeRepo.save(layerNode);
            return GeneralResult.builder().status("success").message("update category successfully").build();
        }catch (Exception e){
            return GeneralResult.builder().status("error").message("failed to update category").build();
        }
    }

    public boolean updateLayer(LayerNode layerNode, InfoDTO infoDTO){
        try{
            String parent_id = infoDTO.getParent_id();
            if (parent_id != null && !parent_id.isEmpty()){
                LayerNode parentNode = getLayerNodeById(parent_id);
                layerNode.setPath(getNodePath(parentNode));
            }
            String layerName = infoDTO.getLayerName();
            if (layerName != null && !layerName.isEmpty()){
                layerNode.setLayerName(layerName);
            }
            Map<String, String> usage = infoDTO.getUsage();
            if (usage != null){
                layerNode.setUsage(usage);
            }
            layerNode.setUpdatedAt(System.currentTimeMillis());
            saveLayerNode(layerNode);
            return true;
        }catch (Exception e){
            return false;
        }
    }
}
