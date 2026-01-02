package com.panorama.backend.model.resource;

import lombok.Builder;
import lombok.Data;

/**
 * @author: DMK
 * @description:
 * @date: 2024-09-27 21:03:51
 * @version: 1.0
 */
@Data
@Builder
public class GeneralResult {
    private String code;
    private String status;
    private Object message;
    private Object data;
}
