package com.yanglizi.aigc.pojo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class DoneChatPayload extends ChatPayload {
    private Integer action;
    private String message; // 可选提示信息
}