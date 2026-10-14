package com.yanglizi.diary.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class genDiaryDto {
    private String content;
    private List<String> emotions;
    private Integer emotionScore;
}
