package com.yanglizi.diary.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiarySearchResult {
    private Integer id;
    private String content;
    private Integer userId;
    private Integer emotionScore;
    private Double vectorScore;     // 向量相似度（原始分数）
    private Double fullTextScore;   // 全文检索分数（原始分数）
    private Double finalScore;      // RRF 融合后的最终得分
    private String source;          // 'fulltext' / 'vector' / 'both'
}