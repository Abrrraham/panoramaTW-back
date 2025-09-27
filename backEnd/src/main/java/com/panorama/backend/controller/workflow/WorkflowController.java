package com.panorama.backend.controller.workflow;

import com.panorama.backend.service.workflow.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: DMK
 * @description:
 * @date: 2025-03-12 23:00:42
 * @version: 1.0
 */
@RestController
@RequestMapping("api/v0/workflow")
public class WorkflowController {

    private final WorkflowService workflowService;

    @Autowired
    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping("/execute")
    public ResponseEntity<?> executeWorkflow(@RequestBody String jsonInput) {
        try {
            workflowService.executeWorkflow(jsonInput);
            return ResponseEntity.ok("工作流执行完成");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("执行失败: " + e.getMessage());
        }
    }
}
