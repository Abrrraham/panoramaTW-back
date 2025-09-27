package com.panorama.backend.config;

import org.springframework.stereotype.Component;

/**
 * @author: DMK
 * @date: 2024-09-18 18:52:08
 * @description:
 * @version: 1.0
 */
@Component
public class DataSourceContextHolder {
    private static final ThreadLocal<String> dataSourceKey = new ThreadLocal<String>();

    //切换数据源
    public static void setDataSourceKey(String key) {
        dataSourceKey.set(key);
    }

    //获取数据源
    public static String getDataSourceKey() {
        return dataSourceKey.get();
    }

    //重置数据源
    public static void clearDataSourceKey() {
        dataSourceKey.remove();
    }
}
