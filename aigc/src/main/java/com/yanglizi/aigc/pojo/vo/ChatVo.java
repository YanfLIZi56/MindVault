package com.yanglizi.aigc.pojo.vo;

import com.yanglizi.aigc.enums.EventType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatVo {
    private Object data;
    private EventType event;

}
