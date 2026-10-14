package com.yanglizi.diary.pojo.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class QueryDiaryDto {
    private Integer userId;
    private List<String> emotions;
    private Date startDiaryDate;
    private Date endDiaryDate;
    private Integer emotionScore;
    private Integer status;
    private Integer pageSize = 10;
    private Integer pageNum = 1;
}
