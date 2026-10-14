package com.yanglizi.diary.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DiaryVo {

    private Integer id;
    private String diaryDate;
    private Integer sourceType;
    private List<String> emotions;
    private Integer emotionScore;
    private String content;
    private String versionGroup;
    private Integer version;
    private Integer isCurrent;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer status;
    private String title;  // 会话标题
}
