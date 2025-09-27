package com.panorama.backend.config;

import com.panorama.backend.model.resource.DefaultDataSource;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: DMK
 * @date: 2024-09-18 18:52:08
 * @description:
 * @version: 1.0
 */
@Configuration
@EnableTransactionManagement
@Slf4j
public class DataSourceConfig {

    private final DefaultDataSource defaultDatasource;

    @Autowired
    public DataSourceConfig(DefaultDataSource defaultDatasource) {
        this.defaultDatasource = defaultDatasource;
    }

    //默认基础数据源
    @Bean("default")
    public DataSource defaultDataSource() {
        return DataSourceBuilder.create()
            .driverClassName(defaultDatasource.getDriverClassName())
            .password(defaultDatasource.getPassword())
            .username(defaultDatasource.getUsername())
            .url(defaultDatasource.getUrl())
            .build();
    }

    //自定义动态数据源
    @Bean("dynamicDataSource")
    public DynamicDataSource dynamicDataSource(@Qualifier("default") DataSource defaultDataSource) {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put("default", defaultDataSource);

        // 默认数据源`
        dynamicDataSource.setDefaultTargetDataSource(defaultDataSource);
        // 动态数据源
        dynamicDataSource.setDataSources(dataSourceMap);

        return dynamicDataSource;
    }

    @Bean
    public SqlSessionFactoryBean sqlSessionFactoryBean(@Qualifier("dynamicDataSource") DynamicDataSource dynamicDataSource) throws IOException {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        // 配置自定义动态数据源
        sessionFactory.setDataSource(dynamicDataSource);
        // 开启驼峰转下划线设置
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        sessionFactory.setConfiguration(configuration);
        // 实体、Mapper类映射
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
        return sessionFactory;
    }

    //开启动态数据源@Transactional注解事务管理的支持
    @Bean
    public PlatformTransactionManager transactionManager(@Qualifier("dynamicDataSource") DynamicDataSource dynamicDataSource) {
        return new DataSourceTransactionManager(dynamicDataSource);
    }
}
