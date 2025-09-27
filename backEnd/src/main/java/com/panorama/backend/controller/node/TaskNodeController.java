package com.panorama.backend.controller.node;

import com.panorama.backend.model.Constant.GenerateResultStatus;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.service.node.TaskNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: DMK
 * @description:
 * @date: 2024-10-15 19:52:27
 * @version: 1.0
 */
@RestController
@RequestMapping("api/v0/node/taskNode")
public class TaskNodeController {
    private TaskNodeService taskNodeService;

    @Autowired
    public void setTaskNodeController(TaskNodeService taskNodeService) {
        this.taskNodeService = taskNodeService;
    }

    @GetMapping("/status/{id}")
    public ResponseEntity<GeneralResult> getTaskStatus(@PathVariable String id){
        GeneralResult result;
        try {
            String status = taskNodeService.getTaskStatus(id);
            result = GeneralResult.builder().status(GenerateResultStatus.SUCCESS).message(status).build();
        } catch (Exception e) {
            result = GeneralResult.builder().status(GenerateResultStatus.ERROR).message("fail to get task status").build();
        }
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

}
