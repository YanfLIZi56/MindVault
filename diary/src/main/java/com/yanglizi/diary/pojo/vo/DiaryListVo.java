package com.yanglizi.diary.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiaryListVo {
    private Long total;
    private List<DiaryVo> diaryList;
}
