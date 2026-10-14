package com.yanglizi.aigc.pojo.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Map;

@Data
public class JevResponse {
    private String model;
    private Answers answers;
    private Usage usage;

    @Data
    public static class Answers {
        private IntentItem intent;
    }

    @Data
    public static class IntentItem {
        private String type;
        private String choice;
        private Double confidence;
        private Map<String,Double> probabilities;
    }

    @Data
    public static class Usage {
        @JsonProperty("input_tokens")
        private Integer inputTokens;
        @JsonProperty("output_tokens")
        private Integer outputTokens;
    }
}
