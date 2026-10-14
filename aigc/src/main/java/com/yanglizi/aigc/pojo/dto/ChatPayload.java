package com.yanglizi.aigc.pojo.dto;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public abstract class ChatPayload {

    /** 当前轮次round */
    private Integer round;
}