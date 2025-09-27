package com.panorama.backend.config;

import com.panorama.backend.model.node.LayerNode;
import com.panorama.backend.repository.LayerNodeRepo;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * @author: DMK
 * @description:
 * @date: 2024-11-21 17:17:30
 * @version: 1.0
 */
@Configuration
public class StaticPathConfig implements WebMvcConfigurer {
    @Resource
    LayerNodeRepo layerNodeRepo;

    @Override
    public void addResourceHandlers(@Nonnull ResourceHandlerRegistry registry) {
        List<LayerNode> layerNodes = layerNodeRepo.findLayerNodesByCategory("3DTiles");
        for (LayerNode layerNode : layerNodes) {
            String url = String.join("/", "/api/v0/resource/3DTiles", layerNode.getId(), "**");
            String location = "file:" + layerNode.getDataSource().get("url") + '/' + layerNode.getTableName() + '/';

            registry.addResourceHandler(url) // URL路径
                    .addResourceLocations(location); // 资源位置
//                    .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());
        }
    }
}
