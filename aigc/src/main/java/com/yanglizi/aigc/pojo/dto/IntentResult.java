package com.yanglizi.aigc.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;


public record IntentResult(
        @JsonProperty("intent") String intent,
        @JsonProperty("confidence") double confidence
) {}