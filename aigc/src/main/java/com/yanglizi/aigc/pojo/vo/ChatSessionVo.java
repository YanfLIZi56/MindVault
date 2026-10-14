package com.yanglizi.aigc.pojo.vo;

import lombok.Data;

@Data
public class ChatSessionVo {
    private String id;
    private String title;
    private Integer status;
    private String versionGroup;
    private String startedAt;
    private String updatedAt;
    private String finishedAt;
}
