package com.yanglizi.aigc.pojo.entry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryConversations {
    private String id;
    private String sessionId;
    private Integer round;
    private Integer speaker;
    private String content;
    private String createdAt;

}