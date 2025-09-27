package com.panorama.backend.config;

import com.panorama.backend.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 目录初始化器 - 在应用启动时确保所有必需的目录都存在
 * @author: DMK
 * @description: Directory initializer to ensure all required directories exist on application startup
 * @date: 2025-09-27
 * @version: 1.0
 */
@Slf4j
@Component
public class DirectoryInitializer implements CommandLineRunner {

    @Value("${path.temp}")
    private String tempPath;

    @Value("${path.rasterTile}")
    private String rasterTilePath;

    @Value("${path.rasterFile}")
    private String rasterFilePath;

    @Value("${path.static}")
    private String staticPath;

    @Value("${path.3DTile}")
    private String threeDTilePath;

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing application directories...");
        
        try {
            // 确保所有配置的目录都存在
            FileUtil.ensureDirectoryExists(tempPath);
            FileUtil.ensureDirectoryExists(rasterTilePath);
            FileUtil.ensureDirectoryExists(rasterFilePath);
            FileUtil.ensureDirectoryExists(staticPath);
            FileUtil.ensureDirectoryExists(threeDTilePath);
            
            log.info("All application directories initialized successfully");
        } catch (IOException e) {
            log.error("Failed to initialize application directories: {}", e.getMessage());
            throw new RuntimeException("Application startup failed due to directory initialization error", e);
        }
    }
}
