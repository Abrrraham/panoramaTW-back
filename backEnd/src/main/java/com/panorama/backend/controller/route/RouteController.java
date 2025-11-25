package com.panorama.backend.controller.route;

import com.panorama.backend.model.resource.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v0/route")
public class RouteController {

    @GetMapping("getUserRoutes")
    public ResponseEntity<CommonResponse<Map<String, Object>>> getUserRoutes() {
        // Demo: 返回一个最小可用的路由集合，home 首页布局+视图
        // 前端 `@elegant-router` 期望的结构：name/path/component/meta/children
        Map<String, Object> homeRoute = new HashMap<>();
        homeRoute.put("name", "home");
        homeRoute.put("path", "/home");
        homeRoute.put("component", "layout.base$view.home");
        Map<String, Object> homeMeta = new HashMap<>();
        homeMeta.put("title", "home");
        homeRoute.put("meta", homeMeta);

        Map<String, Object> functionRoute = new HashMap<>();
        functionRoute.put("name", "function");
        functionRoute.put("path", "/function");
        functionRoute.put("component", "layout.base");
        Map<String, Object> functionMeta = new HashMap<>();
        functionMeta.put("title", "function");
        functionRoute.put("meta", functionMeta);

        Map<String, Object> functionRequest = new HashMap<>();
        functionRequest.put("name", "function_request");
        functionRequest.put("path", "/function/request");
        functionRequest.put("component", "view.function_request");
        Map<String, Object> functionRequestMeta = new HashMap<>();
        functionRequestMeta.put("title", "function_request");
        functionRequest.put("meta", functionRequestMeta);
        functionRoute.put("children", List.of(functionRequest));

        List<Map<String, Object>> routes = List.of(homeRoute, functionRoute);

        Map<String, Object> result = new HashMap<>();
        result.put("routes", routes);
        result.put("home", "home");

        return ResponseEntity.ok(CommonResponse.success(result));
    }
}



