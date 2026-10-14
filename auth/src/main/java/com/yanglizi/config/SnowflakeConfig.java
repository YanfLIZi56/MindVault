package com.yanglizi.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SnowflakeConfig {

    @Value("${snow.worker-id}")
    private long workerId;

    @Value("${snow.data-center-id}")
    private long dataCenterId;

    @Bean
    public Snowflake snowflake(){
        // getSnowflake内部是单例缓存，这里交给Spring管理
        return IdUtil.getSnowflake(workerId, dataCenterId);
    }
}