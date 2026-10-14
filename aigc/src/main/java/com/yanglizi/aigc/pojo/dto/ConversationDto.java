package com.yanglizi.aigc.pojo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConversationDto {
    private Integer round;
    private String content;
    private String role;
}
