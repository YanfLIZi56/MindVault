package com.yanglizi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "cors")
@Data
public class CorsProperties {
    private List<String> allowOrigins;

}