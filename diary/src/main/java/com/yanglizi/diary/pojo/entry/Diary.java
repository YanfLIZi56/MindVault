package com.yanglizi.diary.pojo.entry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Diary {
    private Integer id;
    private Integer userId;
    private Date diaryDate;
    private Integer sourceType;
    private List<String> emotions;
    private Integer emotionScore;
    private String content;
    private String versionGroup;
    private Integer version;
    private Integer isCurrent;
    private LocalDateTime confirmedAt;
    private Long deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer status;
}
