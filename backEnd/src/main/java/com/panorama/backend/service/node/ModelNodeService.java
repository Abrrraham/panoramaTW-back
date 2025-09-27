package com.panorama.backend.service.node;

import com.panorama.backend.model.node.ModelNode;
import com.panorama.backend.repository.ModelNodeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author: DMK
 * @description:
 * @date: 2024-10-16 22:05:41
 * @version: 1.0
 */
@Service
public class ModelNodeService {
    private ModelNodeRepo modelNodeRepo;

    @Autowired
    public void setModelNodeRepo(ModelNodeRepo modelNodeRepo) {
        this.modelNodeRepo = modelNodeRepo;
    }

    public ModelNode getModelNodeByName(String name){
        return modelNodeRepo.findModelNodeByName(name);
    }

}
