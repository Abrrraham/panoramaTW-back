package com.panorama.backend.service.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panorama.backend.model.node.LayerNode;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.model.workflow.WorkflowEdge;
import com.panorama.backend.model.workflow.WorkflowNode;
import com.panorama.backend.service.map.VectorTileService;
import com.panorama.backend.service.node.LayerNodeService;
import com.panorama.backend.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 * @author: DMK
 * @description:
 * @date: 2025-03-12 22:54:47
 * @version: 1.0
 */
@Service
@Slf4j
public class WorkflowService {

    private final VectorTileService vectorTileService;
    private final LayerNodeService layerNodeService;

    @Autowired
    public WorkflowService(VectorTileService vectorTileService, LayerNodeService layerNodeService) {
        this.vectorTileService = vectorTileService;
        this.layerNodeService = layerNodeService;
    }

    public void executeWorkflow(String jsonInput) throws Exception {
        // 解析 JSON
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonInput);

        // 提取 node 和 edge
        List<WorkflowNode> nodes = new ArrayList<>();
        for (JsonNode node : rootNode.get("node")) {
            WorkflowNode workflowNode = objectMapper.treeToValue(node, WorkflowNode.class);
            nodes.add(workflowNode);
        }

        List<WorkflowEdge> edges = new ArrayList<>();
        for (JsonNode edge : rootNode.get("edge")) {
            WorkflowEdge workflowEdge = objectMapper.treeToValue(edge, WorkflowEdge.class);
            edges.add(workflowEdge);
        }

        // 按拓扑顺序执行
        Map<String, String> results = new HashMap<>();
        for (WorkflowNode node : nodes) {
            executeNode(node, edges, results);
        }
        System.out.println("Workflow execution completed.");
        System.out.println("Results: " + results);
    }

    private void executeNode(WorkflowNode node, List<WorkflowEdge> edges, Map<String, String> results) throws Exception {
        Map<String, Object> params = node.getParams();

        // 替换输入参数
        for (WorkflowEdge edge : edges) {
            if (edge.getEnd_node().equals(node.getNode_id())) {
                for (Map.Entry<String, String> entry : edge.getMap().entrySet()) {
                    String previousOutput = results.get(entry.getKey());
                    params.put(entry.getValue(), previousOutput);
                }
            }
        }

        // 处理数据库读取逻辑
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() instanceof Map) {
                Map<String, Object> valueMap = (Map<String, Object>) entry.getValue();
                if (valueMap.containsKey("data_id")) {
                    String tableName = (String) valueMap.get("data_id");
                    LayerNode layerNode = layerNodeService.getLayerNodeByTableName(tableName); // 查询数据库
                    GeneralResult geojsonResult = vectorTileService.getGeojsonByTableName(layerNode, tableName); // 查询数据库
                    if (!geojsonResult.getStatus().equals("success")) {
                        throw new Exception("Failed to get GeoJSON from table " + tableName);
                    }
                    String geojson = (String) geojsonResult.getMessage();
                    params.put(entry.getKey(), geojson); // 替换参数
                }
            }
        }
        System.out.println(params);
        // 执行 Python 脚本
        String result = callPythonScript(node.getModel_name(), params);
        // 记录执行结果
        for (String outputKey : node.getOutput()) {
            results.put(outputKey, result);
        }
    }

    public String callPythonScript(String modelName, Map<String, Object> params) throws Exception {
        String pythonScript = "H:\\data\\panoramaTW\\script\\" + modelName + ".py";

        // 构造命令参数列表
        List<String> command = new ArrayList<>();
//        command.add("cmd.exe");
//        command.add("/c");
//        command.add("conda activate bankModel &&");
//        command.add("python");
        command.add("F:\\App\\anaconda3\\envs\\bankModel\\python.exe");
        command.add(pythonScript);

//        JsonNode paramsJson = JsonUtil.mapToJson(params);
//        // 遍历 params，转换为 `--key value` 形式
//        paramsJson.fields().forEachRemaining(entry -> {
//            command.add("--" + entry.getKey()); // 添加参数名，例如 `--buffer_distance`
//            if (entry.getValue().isNumber()){
//                command.add(String.valueOf(entry.getValue()));
//            }else {
//                JsonNode valueNode = entry.getValue();
//                // 输出原始 JSON 值
//                System.out.println(JsonUtil.toJsonString(valueNode));
//                // 确保值按 JSON 形式存入命令
//                command.add(JsonUtil.toJsonString(valueNode));
//            }
//        });
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            command.add("--" + entry.getKey()); // 添加参数名，例如 --buffer_distance
            command.add(JsonUtil.serializeObject(entry.getValue()));
        }

        log.info("Executing command: {}", String.join(" ", command));

        // 使用 ProcessBuilder 调用 Python 脚本
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            process.waitFor();
            return output.toString();
        }
    }
}


