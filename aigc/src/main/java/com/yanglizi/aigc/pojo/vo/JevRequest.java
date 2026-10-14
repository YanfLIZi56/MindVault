package com.yanglizi.aigc.pojo.vo;

import lombok.Data;
import java.util.Map;

@Data
public class JevRequest {
    private String model;
    private Map<String, QuestionItem> questions;
    private String state;

    @Data
    public static class QuestionItem {
        private String type;
        private String instructions;
        private Map<String,String> criteria;
    }
}