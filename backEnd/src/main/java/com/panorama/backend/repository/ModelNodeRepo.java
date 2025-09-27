package com.panorama.backend.repository;

import com.panorama.backend.model.node.ModelNode;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * @author: DMK
 * @description:
 * @date: 2024-10-16 22:06:05
 * @version: 1.0
 */
@Repository
public interface ModelNodeRepo extends MongoRepository<ModelNode, String> {

    ModelNode findModelNodeByName(String name);

}
