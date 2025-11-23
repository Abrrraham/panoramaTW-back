package com.panorama.backend.DTO.system;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class SystemMenuDTO {
    private long id;
    private String createBy;
    private String createTime;
    private String updateBy;
    private String updateTime;
    private String status;
    private long parentId;
    private String menuType;
    private String menuName;
    private String routeName;
    private String routePath;
    private String component;
    private String icon;
    private String iconType;
    private Boolean keepAlive;
    private Boolean constant;
    private Integer order;
    private String href;
    private Boolean hideInMenu;
    private String activeMenu;
    private Boolean multiTab;
    private Integer fixedIndexInTab;
    private Map<String, Object> query;
    private String i18nKey;
    private List<MenuButtonDTO> buttons;
    private List<SystemMenuDTO> children;
}
