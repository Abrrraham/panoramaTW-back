package com.panorama.backend.repository;

import com.panorama.backend.model.node.TaskNode;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author: DMK
 * @description:
 * @date: 2024-10-14 22:15:25
 * @version: 1.0
 */
public interface TaskNodeRepo extends MongoRepository<TaskNode, String> {

    TaskNode findTaskNodeById(String id);

}
