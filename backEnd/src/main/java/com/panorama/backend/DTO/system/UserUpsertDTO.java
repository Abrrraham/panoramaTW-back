package com.panorama.backend.DTO.system;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserUpsertDTO {
    private String id;
    @NotBlank
    private String userName;
    private String password;
    private String userGender;
    private String nickName;
    private String userPhone;
    private String userEmail;
    private String status;
    private List<String> userRoles = new ArrayList<>();

    /**
     * 兼容前端传单个字符串或数组两种情况
     */
    @JsonSetter("userRoles")
    public void setUserRolesFlexible(Object roles) {
        if (roles == null) {
            this.userRoles = new ArrayList<>();
        } else if (roles instanceof List<?> list) {
            this.userRoles = list.stream().map(String::valueOf).toList();
        } else {
            this.userRoles = List.of(String.valueOf(roles));
        }
    }
}
