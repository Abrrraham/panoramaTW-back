package com.panorama.backend.service.resource;

/**
 * @author: DMK
 * @description:
 * @date: 2024-10-14 22:05:59
 * @version: 1.0
 */

import com.panorama.backend.model.Constant.TaskStatus;
import com.panorama.backend.model.Constant.TaskType;
import com.panorama.backend.model.node.LayerNode;
import com.panorama.backend.model.node.TaskNode;
import com.panorama.backend.service.node.LayerNodeService;
import com.panorama.backend.service.node.TaskNodeService;
import com.panorama.backend.util.FileUtil;
import com.panorama.backend.util.ProcessUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

@Component
@Slf4j
public class AsyncTaskService {

    private TaskNodeService taskNodeService;
    private LayerNodeService layerNodeService;

    @Autowired
    public void setAsyncTaskService(TaskNodeService taskNodeService, LayerNodeService layerNodeService) {
        this.taskNodeService = taskNodeService;
        this.layerNodeService = layerNodeService;
    }

    @Async
    public void systemTaskAsync(String taskNodeId) {
        TaskNode taskNode = taskNodeService.getTaskNodeById(taskNodeId);
        try {

            taskNodeService.updateTaskStatus(taskNode, TaskStatus.START);
            Process process = ProcessUtil.buildSystemProcess(taskNode);
            manageProcess(taskNode, process);
        } catch (Exception e) {
            taskNodeService.updateTaskStatus(taskNode, TaskStatus.ERROR);
        }
    }

    @Async
    public void modelTaskAsync(String taskNodeId) {
        TaskNode taskNode = taskNodeService.getTaskNodeById(taskNodeId);
        try {
            taskNodeService.updateTaskStatus(taskNode, TaskStatus.START);
            Process process = ProcessUtil.buildModelProcess(taskNode);
            manageProcess(taskNode, process);

        } catch (Exception e) {
            taskNodeService.updateTaskStatus(taskNode, TaskStatus.ERROR);
        }
    }

    private void manageProcess(TaskNode taskNode, Process process) throws InterruptedException {
        if (process != null) {
            getProcessOutput(process);
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                taskNodeService.updateTaskStatus(taskNode, TaskStatus.COMPLETE);
                LayerNode layerNode = taskNode.getLayerNode();
                if (layerNode != null) {
                    if (taskNode.getType().equals(TaskType.UPLOAD)){
                        layerNodeService.saveLayerNode(layerNode);
                    }else if (taskNode.getType().equals(TaskType.DELETE)){
                        layerNodeService.deleteLayerNode(layerNode);
                    }
                }
            }else {
                taskNodeService.updateTaskStatus(taskNode, TaskStatus.ERROR);
            }
        }else {
            taskNodeService.updateTaskStatus(taskNode, TaskStatus.ERROR);
        }
        String tempPath = taskNode.getTempPath();
        if (tempPath != null) {
            FileUtil.deleteDirectory(Path.of(tempPath));
        }
    }

    private static void getProcessOutput(Process process) {
        try {
            // 获取进程的标准输出流
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            // 逐行读取输出
            while ((line = reader.readLine()) != null) {
                log.info(line);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
