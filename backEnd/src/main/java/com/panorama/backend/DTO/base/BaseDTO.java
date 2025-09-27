package com.panorama.backend.DTO.base;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: Steven Da
 * @date: 2024/10/03/10:19
 * @description:
 */
@Data
@SuperBuilder
@NoArgsConstructor
public class BaseDTO {
    String id;
    String tableName;
    String layerName;
    Map<String, String> usage;
}
