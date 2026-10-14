package com.yanglizi.aigc.pojo.entry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSession {

    private String id;
    private Integer userId;
    private String title;
    private Integer status;
    private String versionGroup;
    private Integer lastRound;
    private String rawFacts;
    private Integer rawFactsLastRound;
    private Integer rawFactsPendingRound;
    private String startedAt;
    private String updatedAt;
    private String finishedAt;

}
