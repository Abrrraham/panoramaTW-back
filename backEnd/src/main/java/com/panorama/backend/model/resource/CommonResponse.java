package com.panorama.backend.model.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonResponse<T> {
    private String code; // e.g. 0000
    private String msg;  // e.g. ok
    private T data;

    public static <T> CommonResponse<T> success(T data) {
        return CommonResponse.<T>builder().code("0000").msg("ok").data(data).build();
    }

    public static <T> CommonResponse<T> fail(String code, String msg) {
        return CommonResponse.<T>builder().code(code).msg(msg).build();
    }
}



