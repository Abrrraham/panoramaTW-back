package com.panorama.backend.repository;

import com.panorama.backend.model.node.LayerNode;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author: DMK
 * @description:
 * @date: 2024-09-14 15:05:16
 * @version: 1.0
 */
@Repository
public interface LayerNodeRepo extends MongoRepository<LayerNode, String> {

    LayerNode findLayerNodeById(String id);

    LayerNode findLayerNodeByTableName(String tableName);

    List<LayerNode> findLayerNodesByPath(String path);

    List<LayerNode> findLayerNodesByPathStartingWith(String prefix);

    List<LayerNode> findLayerNodesByCategory(String category);

}
