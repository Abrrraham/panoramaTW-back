package com.panorama.backend.DTO.system;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MenuTreeNodeDTO {
    private long id;
    private String label;
    private long pId;
    private List<MenuTreeNodeDTO> children;
}
