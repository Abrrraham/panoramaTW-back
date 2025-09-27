package com.panorama.backend.model.node;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: Steven Da
 * @date: 2024/09/14/11:14
 * @description:
 */
@Document(collection = "layerNode")
@Data
@Builder
public class LayerNode {
    @Id
    private String id;

    private Map<String, String> dataSource;

    private String category;

    @Indexed
    private String path;

    private String tableName;

    private String layerName;

    private Map<String, String> usage;
}
