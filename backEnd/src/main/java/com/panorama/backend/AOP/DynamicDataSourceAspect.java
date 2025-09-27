package com.panorama.backend.AOP;

import com.panorama.backend.config.DataSourceContextHolder;
import com.panorama.backend.config.DynamicDataSource;
import com.panorama.backend.model.node.LayerNode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: DMK
 * @date: 2024-09-18 18:52:08
 * @description:
 * @version: 1.0
 */
@Slf4j
@Aspect
@Component
public class DynamicDataSourceAspect {

    private DynamicDataSource dynamicDataSource;

    //数据库对应驱动
    private final Map<String, String> datasourceDriverMapper = new HashMap<>(){{
        put("mysql", "com.mysql.cj.jdbc.Driver");
        put("oracle", "oracle.jdbc.driver.OracleDriver");
        put("postgresql", "org.postgresql.Driver");
        put("sqlite", "org.sqlite.JDBC");
        put("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }};

    @Qualifier("dynamicDataSource")
    @Autowired
    public void setDynamicDataSource(DynamicDataSource dynamicDataSource) {
        this.dynamicDataSource = dynamicDataSource;
    }

    //切换数据源
    @Around("@annotation(com.panorama.backend.annotation.DynamicNodeData)")
    public Object switchDataSource(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        LayerNode layerNode = (LayerNode) args[0];

        try {
            String hashCode4JDBC= String.valueOf(layerNode.getDataSource().get("url").hashCode());
            //判断数据源是否已存在，若不存在则新建
            if (!dynamicDataSource.containDataSourceKey(hashCode4JDBC)) {
                DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
                String url = layerNode.getDataSource().get("url");
                dataSourceBuilder.url(url);
                //根据驱动map得到对应数据库的驱动
                dataSourceBuilder.driverClassName(datasourceDriverMapper.get(url.split(":")[1]));
                if(layerNode.getDataSource().containsKey("username")) {
                    dataSourceBuilder.username(layerNode.getDataSource().get("username"));
                }
                if(layerNode.getDataSource().containsKey("password")) {
                    dataSourceBuilder.password(layerNode.getDataSource().get("password"));
                }
                DataSource source = dataSourceBuilder.build();
                log.info("add datasource: {}", source.toString());
                dynamicDataSource.addDataSource(hashCode4JDBC, source);
            }
            // 切换数据源
            DataSourceContextHolder.setDataSourceKey(hashCode4JDBC);
        } catch (Exception e) {
            DataSourceContextHolder.setDataSourceKey("default");
        }
        log.info(DataSourceContextHolder.getDataSourceKey());

        Object result = joinPoint.proceed();

        DataSourceContextHolder.setDataSourceKey("default");

        return result;
    }

}
