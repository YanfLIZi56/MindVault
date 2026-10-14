package com.yanglizi.level.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiLevelConfig {
    private Integer id;
    private String apiKey;
    private String levelCode;
    private String description;
    private String updatedAt;
}
