package com.panorama.backend.model.node;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

/**
 * @author: DMK
 * @description:
 * @date: 2024-10-14 22:10:41
 * @version: 1.0
 */
@Document(collection = "taskNode")
@Data
@Builder
public class TaskNode {
    @Id
    private String id;
    @DBRef
    ModelNode modelNode;
    LayerNode layerNode;
    private String status;
    private Map<String, String> params;
    private String tempPath;
    private String type;
}
