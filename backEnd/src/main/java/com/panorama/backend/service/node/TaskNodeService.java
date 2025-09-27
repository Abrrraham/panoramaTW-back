package com.panorama.backend.service.node;

import com.panorama.backend.model.node.TaskNode;
import com.panorama.backend.repository.TaskNodeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author: DMK
 * @description:
 * @date: 2024-10-14 22:16:54
 * @version: 1.0
 */
@Service
public class TaskNodeService {
    private TaskNodeRepo taskNodeRepo;

    @Autowired
    public void setTaskNodeRepo(TaskNodeRepo taskNodeRepo) {
        this.taskNodeRepo = taskNodeRepo;
    }

    public TaskNode getTaskNodeById(String id){
        return taskNodeRepo.findTaskNodeById(id);
    }

    public String saveTaskNode(TaskNode taskNode){
        return taskNodeRepo.save(taskNode).getId();
    }

    public void updateTaskStatus(TaskNode taskNode, String status){
        taskNode.setStatus(status);
        taskNodeRepo.save(taskNode);
    }

    public String getTaskStatus(String id){
        return taskNodeRepo.findTaskNodeById(id).getStatus();
    }

}
