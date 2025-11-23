package com.panorama.backend.controller.admin;

import com.panorama.backend.DTO.common.ApiResponse;
import com.panorama.backend.DTO.system.MenuButtonDTO;
import com.panorama.backend.DTO.system.MenuTreeNodeDTO;
import com.panorama.backend.DTO.system.SimpleRoleDTO;
import com.panorama.backend.DTO.system.SystemMenuDTO;
import com.panorama.backend.DTO.system.SystemPaginatedResponse;
import com.panorama.backend.DTO.system.SystemRoleDTO;
import com.panorama.backend.DTO.system.SystemUserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("api/v0/systemManage")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class SystemManageController {

    private static final String NOW = LocalDateTime.now().toString();

    private static final List<SystemRoleDTO> ROLE_DATA = List.of(
            SystemRoleDTO.builder()
                    .id(1L)
                    .createBy("system")
                    .createTime(NOW)
                    .updateBy("system")
                    .updateTime(NOW)
                    .status("1")
                    .roleName("管理员")
                    .roleCode("ROLE_ADMIN")
                    .roleDesc("系统管理员，拥有全部权限")
                    .build(),
            SystemRoleDTO.builder()
                    .id(2L)
                    .createBy("system")
                    .createTime(NOW)
                    .updateBy("system")
                    .updateTime(NOW)
                    .status("1")
                    .roleName("普通用户")
                    .roleCode("ROLE_USER")
                    .roleDesc("仅可浏览地图数据")
                    .build()
    );

    private static final List<SystemUserDTO> USER_DATA = List.of(
            SystemUserDTO.builder()
                    .id(1001L)
                    .createBy("system")
                    .createTime(NOW)
                    .updateBy("system")
                    .updateTime(NOW)
                    .status("1")
                    .userName("admin")
                    .userGender("1")
                    .nickName("系统管理员")
                    .userPhone("13800000000")
                    .userEmail("admin@example.com")
                    .userRoles(List.of("ROLE_ADMIN"))
                    .build(),
            SystemUserDTO.builder()
                    .id(1002L)
                    .createBy("system")
                    .createTime(NOW)
                    .updateBy("system")
                    .updateTime(NOW)
                    .status("1")
                    .userName("viewer")
                    .userGender("1")
                    .nickName("地图观察者")
                    .userPhone("13800000001")
                    .userEmail("viewer@example.com")
                    .userRoles(List.of("ROLE_USER"))
                    .build()
    );

    private static final List<SystemMenuDTO> MENU_DATA = List.of(
            SystemMenuDTO.builder()
                    .id(1L)
                    .createBy("system")
                    .createTime(NOW)
                    .updateBy("system")
                    .updateTime(NOW)
                    .status("1")
                    .parentId(0L)
                    .menuType("1")
                    .menuName("系统管理")
                    .routeName("manage")
                    .routePath("/manage")
                    .component("layout.base")
                    .icon("carbon:cloud-service-management")
                    .iconType("1")
                    .keepAlive(true)
                    .constant(false)
                    .order(9)
                    .href(null)
                    .hideInMenu(false)
                    .activeMenu(null)
                    .multiTab(false)
                    .fixedIndexInTab(null)
                    .query(null)
                    .i18nKey("route.manage")
                    .children(List.of(
                            SystemMenuDTO.builder()
                                    .id(11L)
                                    .createBy("system")
                                    .createTime(NOW)
                                    .updateBy("system")
                                    .updateTime(NOW)
                                    .status("1")
                                    .parentId(1L)
                                    .menuType("2")
                                    .menuName("用户管理")
                                    .routeName("manage_user")
                                    .routePath("/manage/user")
                                    .component("view.manage_user")
                                    .icon("ic:round-manage-accounts")
                                    .iconType("1")
                                    .keepAlive(true)
                                    .constant(false)
                                    .order(1)
                                    .i18nKey("route.manage_user")
                                    .buttons(List.of(
                                            MenuButtonDTO.builder().code("user:add").desc("新增用户").build(),
                                            MenuButtonDTO.builder().code("user:edit").desc("编辑用户").build()
                                    ))
                                    .build(),
                            SystemMenuDTO.builder()
                                    .id(12L)
                                    .createBy("system")
                                    .createTime(NOW)
                                    .updateBy("system")
                                    .updateTime(NOW)
                                    .status("1")
                                    .parentId(1L)
                                    .menuType("2")
                                    .menuName("角色管理")
                                    .routeName("manage_role")
                                    .routePath("/manage/role")
                                    .component("view.manage_role")
                                    .icon("carbon:user-role")
                                    .iconType("1")
                                    .keepAlive(true)
                                    .constant(false)
                                    .order(2)
                                    .i18nKey("route.manage_role")
                                    .buttons(List.of(
                                            MenuButtonDTO.builder().code("role:add").desc("新增角色").build(),
                                            MenuButtonDTO.builder().code("role:edit").desc("编辑角色").build()
                                    ))
                                    .build(),
                            SystemMenuDTO.builder()
                                    .id(13L)
                                    .createBy("system")
                                    .createTime(NOW)
                                    .updateBy("system")
                                    .updateTime(NOW)
                                    .status("1")
                                    .parentId(1L)
                                    .menuType("2")
                                    .menuName("菜单管理")
                                    .routeName("manage_menu")
                                    .routePath("/manage/menu")
                                    .component("view.manage_menu")
                                    .icon("material-symbols:route")
                                    .iconType("1")
                                    .keepAlive(true)
                                    .constant(false)
                                    .order(3)
                                    .i18nKey("route.manage_menu")
                                    .buttons(List.of(
                                            MenuButtonDTO.builder().code("menu:add").desc("新增菜单").build()
                                    ))
                                    .build()
                    ))
                    .build()
    );

    private static final List<MenuTreeNodeDTO> MENU_TREE = List.of(
            MenuTreeNodeDTO.builder()
                    .id(1L)
                    .label("系统管理")
                    .pId(0L)
                    .children(List.of(
                            MenuTreeNodeDTO.builder().id(11L).label("用户管理").pId(1L).build(),
                            MenuTreeNodeDTO.builder().id(12L).label("角色管理").pId(1L).build(),
                            MenuTreeNodeDTO.builder().id(13L).label("菜单管理").pId(1L).build()
                    ))
                    .build()
    );

    @GetMapping("/getRoleList")
    public ApiResponse<SystemPaginatedResponse<SystemRoleDTO>> getRoleList(
            @RequestParam(value = "current", required = false, defaultValue = "1") long current,
            @RequestParam(value = "size", required = false, defaultValue = "10") long size) {
        return ApiResponse.success(paginate(ROLE_DATA, current, size));
    }

    @GetMapping("/getAllRoles")
    public ApiResponse<List<SimpleRoleDTO>> getAllRoles() {
        List<SimpleRoleDTO> list = ROLE_DATA.stream()
                .map(role -> SimpleRoleDTO.builder()
                        .id(role.getId())
                        .roleName(role.getRoleName())
                        .roleCode(role.getRoleCode())
                        .build())
                .toList();
        return ApiResponse.success(list);
    }

    @GetMapping("/getUserList")
    public ApiResponse<SystemPaginatedResponse<SystemUserDTO>> getUserList(
            @RequestParam(value = "current", required = false, defaultValue = "1") long current,
            @RequestParam(value = "size", required = false, defaultValue = "10") long size) {
        return ApiResponse.success(paginate(USER_DATA, current, size));
    }

    @GetMapping("/getMenuList/v2")
    public ApiResponse<SystemPaginatedResponse<SystemMenuDTO>> getMenuList(
            @RequestParam(value = "current", required = false, defaultValue = "1") long current,
            @RequestParam(value = "size", required = false, defaultValue = "10") long size) {
        return ApiResponse.success(paginate(flattenMenus(MENU_DATA), current, size));
    }

    @GetMapping("/getAllPages")
    public ApiResponse<List<String>> getAllPages() {
        return ApiResponse.success(List.of(
                "view.main",
                "view.layout",
                "view.manage_user",
                "view.manage_role",
                "view.manage_menu",
                "view.admin-data-manage"));
    }

    @GetMapping("/getMenuTree")
    public ApiResponse<List<MenuTreeNodeDTO>> getMenuTree() {
        return ApiResponse.success(MENU_TREE);
    }

    private <T> SystemPaginatedResponse<T> paginate(List<T> data, long current, long size) {
        long total = data.size();
        long fromIndex = Math.max((current - 1) * size, 0);
        long toIndex = Math.min(fromIndex + size, total);
        List<T> records = fromIndex >= total ? List.of() : data.subList((int) fromIndex, (int) toIndex);
        return SystemPaginatedResponse.<T>builder()
                .current(current)
                .size(size)
                .total(total)
                .records(records)
                .build();
    }

    private List<SystemMenuDTO> flattenMenus(List<SystemMenuDTO> menus) {
        java.util.List<SystemMenuDTO> result = new java.util.ArrayList<>();
        for (SystemMenuDTO menu : menus) {
            result.add(menu);
            if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
                result.addAll(flattenMenus(menu.getChildren()));
            }
        }
        return result;
    }
}
