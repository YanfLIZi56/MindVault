package com.yanglizi.aigc.pojo.entry;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IntentKeywordConfig {
    private Long id;
    private String intentType;
    private String keyword;
    private Boolean enabled;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}