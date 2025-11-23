package com.panorama.backend.DTO.system;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemRoleDTO {
    private long id;
    private String createBy;
    private String createTime;
    private String updateBy;
    private String updateTime;
    private String status;
    private String roleName;
    private String roleCode;
    private String roleDesc;
}
