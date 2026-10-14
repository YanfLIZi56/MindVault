package com.yanglizi.aigc.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatHistoryCursorVO<T> {
    /** 当前批次消息列表 */
    private List<T> records;
    /** 是否还有更多历史可以上滑加载 */
    private Boolean hasMore;
}
