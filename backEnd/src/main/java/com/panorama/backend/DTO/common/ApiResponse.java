package com.panorama.backend.DTO.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiResponse<T> {
    private String code;
    private String msg;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code("0000")
                .msg("success")
                .data(data)
                .build();
    }
}
