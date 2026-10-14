package com.yanglizi.diary.pojo.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class DiaryDto {
    private Integer id;
    private Date diaryDate;
    private List<String> emotions;
    private String content;
    private String versionGroup;
}
