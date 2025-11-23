package com.panorama.backend.DTO.system;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SimpleRoleDTO {
    private long id;
    private String roleName;
    private String roleCode;
}
