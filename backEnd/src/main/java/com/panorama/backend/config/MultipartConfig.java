package com.panorama.backend.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import jakarta.servlet.MultipartConfigElement;
import java.io.File;

/**
 * 文件上传配置
 * @author: DMK
 * @description: 配置文件上传的相关参数，包括临时目录、文件大小限制等
 * @date: 2024-09-27
 * @version: 1.0
 */
@Configuration
public class MultipartConfig {

    /**
     * 配置文件上传参数
     * @return MultipartConfigElement
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        
        // 设置文件上传的临时目录
        String tempDir = System.getProperty("java.io.tmpdir");
        File uploadTempDir = new File(tempDir, "spring-upload");
        if (!uploadTempDir.exists()) {
            uploadTempDir.mkdirs();
        }
        factory.setLocation(uploadTempDir.getAbsolutePath());
        
        // 设置单个文件的最大大小 (1GB)
        factory.setMaxFileSize(DataSize.ofGigabytes(1));
        
        // 设置总上传数据的最大大小 (1GB)  
        factory.setMaxRequestSize(DataSize.ofGigabytes(1));
        
        // 设置内存中保存文件的临界值，超过这个大小的文件将被写入磁盘
        factory.setFileSizeThreshold(DataSize.ofMegabytes(10));
        
        return factory.createMultipartConfig();
    }
}
