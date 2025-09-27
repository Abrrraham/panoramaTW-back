package com.panorama.backend.model.node;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * @author: DMK
 * @description:
 * @date: 2024-10-16 21:51:59
 * @version: 1.0
 */
@Document(collection = "modelNode")
@Data
@Builder
public class ModelNode {
    @Id
    private String id;
    private String name;
    private String program;
    private List<String> paramKey;
    private List<String> output;
    private String exePrefix;
    private String condaEnv;
}
