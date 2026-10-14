package com.yanglizi.level.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LevelCacheDto {
    private Integer id;
    private Integer code;
    private String name;
    private Integer ruleMin;
    private String iconUrl;
}
