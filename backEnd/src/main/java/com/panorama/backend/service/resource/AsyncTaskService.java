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
import com.panorama.backend.model.Constant.LayerStatus;
import com.panorama.backend.service.node.LayerNodeService;
import com.panorama.backend.service.node.TaskNodeService;
import com.panorama.backend.util.FileUtil;
import com.panorama.backend.util.ProcessUtil;
import com.panorama.backend.util.RasterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Map;

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
            String output = collectProcessOutput(process);
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                taskNodeService.updateTaskStatus(taskNode, TaskStatus.COMPLETE);
                LayerNode layerNode = taskNode.getLayerNode();
                if (layerNode != null) {
                    if (taskNode.getType().equals(TaskType.UPLOAD)){
                        updateLayerOnUploadSuccess(taskNode, layerNode);
                    }else if (taskNode.getType().equals(TaskType.DELETE)){
                        layerNodeService.deleteLayerNode(layerNode);
                    }
                }
            }else {
                taskNodeService.updateTaskStatus(taskNode, TaskStatus.ERROR);
                updateLayerOnUploadFailure(taskNode, output);
            }
        }else {
            taskNodeService.updateTaskStatus(taskNode, TaskStatus.ERROR);
            updateLayerOnUploadFailure(taskNode, "process start failed");
        }
        String tempPath = taskNode.getTempPath();
        if (tempPath != null) {
            FileUtil.deleteDirectory(Path.of(tempPath));
        }
    }

    private static String collectProcessOutput(Process process) {
        StringBuilder output = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                log.info(line);
                output.append(line).append('\n');
            }
            BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errReader.readLine()) != null) {
                log.error(line);
                output.append(line).append('\n');
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return output.toString().trim();
    }

    private void updateLayerOnUploadSuccess(TaskNode taskNode, LayerNode layerNode) {
        if (!"raster".equalsIgnoreCase(layerNode.getCategory())) {
            layerNodeService.saveLayerNode(layerNode);
            return;
        }
        Map<String, String> usage = layerNode.getUsage();
        if (usage != null) {
            usage.put("status", LayerStatus.READY);
            usage.put("errorMessage", "");
            String tifPath = taskNode.getParams() == null ? null : taskNode.getParams().get("tifPath");
            String outputPath = taskNode.getParams() == null ? null : taskNode.getParams().get("outputPath");
            double[] bbox = RasterUtil.readGeoTiffBBox(tifPath);
            if (bbox != null) {
                usage.put("bbox", RasterUtil.formatBBox(bbox));
                usage.putIfAbsent("srid", "4326");
            }
            String tileType = RasterUtil.detectTileType(outputPath);
            if (tileType != null && !tileType.isBlank()) {
                usage.put("type", tileType);
            }
        }
        layerNode.setUpdatedAt(System.currentTimeMillis());
        layerNodeService.saveLayerNode(layerNode);
    }

    private void updateLayerOnUploadFailure(TaskNode taskNode, String errorMessage) {
        LayerNode layerNode = taskNode.getLayerNode();
        if (layerNode == null || !"raster".equalsIgnoreCase(layerNode.getCategory())) {
            return;
        }
        Map<String, String> usage = layerNode.getUsage();
        if (usage != null) {
            usage.put("status", LayerStatus.FAILED);
            usage.put("errorMessage", errorMessage == null ? "" : errorMessage);
        }
        layerNode.setUpdatedAt(System.currentTimeMillis());
        layerNodeService.saveLayerNode(layerNode);
    }
}
