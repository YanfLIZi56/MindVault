package com.yanglizi.level.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Level {
    private Integer id;
    private Integer code;
    private String name;
    private Integer ruleMin;
    private String iconUrl;
    private String description;
    private String createdAt;
}
