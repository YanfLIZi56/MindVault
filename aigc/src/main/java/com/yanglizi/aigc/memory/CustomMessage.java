package com.yanglizi.aigc.memory;

import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.content.Media;

import java.util.List;
import java.util.Map;

@Data
public class CustomMessage {
    private MessageType messageType;
    private String textContent;
    private Map<String, Object> metadata = Map.of();
    private List<Media> media = List.of();
    private List<ToolResponseMessage.ToolResponse> toolResponses = List.of();
    private List<AssistantMessage.ToolCall> toolCalls = List.of();

    // 额外添加
    private Map<String, Object> params = Map.of();

}
