package com.panorama.backend.model.workflow;

import lombok.Data;

import java.util.Map;

/**
 * @author: DMK
 * @description:
 * @date: 2025-03-12 22:52:40
 * @version: 1.0
 */
@Data
public class WorkflowEdge {
    private String edge_id;
    private String start_node;
    private String end_node;
    private Map<String, String> map;
}
