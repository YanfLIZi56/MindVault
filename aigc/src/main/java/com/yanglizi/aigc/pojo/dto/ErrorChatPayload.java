package com.yanglizi.aigc.pojo.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class ErrorChatPayload extends ChatPayload {
    private String message;
    private Integer code;
}
