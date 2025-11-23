package com.panorama.backend.DTO.system;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemPaginatedResponse<T> {
    private long current;
    private long size;
    private long total;
    private List<T> records;
}
