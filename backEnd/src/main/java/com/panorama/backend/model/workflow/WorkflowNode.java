package com.panorama.backend.model.workflow;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author: DMK
 * @description:
 * @date: 2025-03-12 22:52:08
 * @version: 1.0
 */
@Data
public class WorkflowNode {
    private String node_id;
    private String node_name;
    private String model_name;
    private Map<String, Object> params;
    private List<String> output;
}
