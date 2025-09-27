package com.panorama.backend.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: DMK
 * @date: 2024-09-18 18:52:08
 * @description:
 * @version: 1.0
 */
public class DynamicDataSource extends AbstractRoutingDataSource {
    //记录数据源的map
    private Map<Object, Object> dataSources = new HashMap<>();

    //获取当前数据源的键
    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSourceKey();
    }

    //设置数据源
    public void setDataSources(Map<Object, Object> dataSources) {
        this.dataSources = dataSources;
        super.setTargetDataSources(dataSources);
    }

    //追加数据源
    public void addDataSource(String key, DataSource dataSource) {
        this.dataSources.put(key, dataSource);
        super.setTargetDataSources(dataSources);
        // 加载新的数据源
        super.afterPropertiesSet();
    }

    public boolean containDataSourceKey(String key) {
        return this.dataSources.containsKey(key);
    }
}
