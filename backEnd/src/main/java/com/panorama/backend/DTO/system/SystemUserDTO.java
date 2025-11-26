package com.panorama.backend.DTO.system;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SystemUserDTO {
    private String mongoId;
    private long id;
    private String createBy;
    private String createTime;
    private String updateBy;
    private String updateTime;
    private String status;
    private String userName;
    private String userGender;
    private String nickName;
    private String userPhone;
    private String userEmail;
    private List<String> userRoles;
    /** expose plain if stored, otherwise hashed */
    private String password;
}
