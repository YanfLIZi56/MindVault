package com.yanglizi.diary.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DiaryIdVo {
    private String sessionId;
    private Integer diaryId;
}
