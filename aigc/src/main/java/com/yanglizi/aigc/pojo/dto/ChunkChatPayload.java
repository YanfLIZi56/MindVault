package com.yanglizi.aigc.pojo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class ChunkChatPayload extends ChatPayload{
    /** AI增量片段 */
    private String delta;
}
