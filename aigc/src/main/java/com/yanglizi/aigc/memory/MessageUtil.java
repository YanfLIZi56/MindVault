package com.yanglizi.aigc.memory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.chat.messages.*;

public class MessageUtil {

    public static String toJson(Message message) {
        CustomMessage bean = BeanUtil.toBean(message, CustomMessage.class);
        bean.setTextContent(message.getText());

        if (message instanceof AssistantMessage assistantMessage){
            bean.setToolCalls(assistantMessage.getToolCalls());
        }

        if (message instanceof ToolResponseMessage toolResponseMessage){
            bean.setToolResponses(toolResponseMessage.getResponses());
        }

        return JSONUtil.toJsonStr(bean);
    }

    public static Message toMessage(String json) {
        CustomMessage bean = JSONUtil.toBean(json, CustomMessage.class);
        MessageType messageType = bean.getMessageType();
        switch (messageType){
            case SYSTEM -> {
                return new SystemMessage(bean.getTextContent());
            }
            case USER -> {
                return UserMessage.builder()
                        .text(bean.getTextContent())
                        .metadata(bean.getMetadata())
                        .media(bean.getMedia())
                        .build();
            }
            case ASSISTANT -> {
                return AssistantMessage.builder()
                        .content(bean.getTextContent())
                        .media(bean.getMedia())
                        .toolCalls(bean.getToolCalls())
                        .build();
            }
            case TOOL -> {
                return ToolResponseMessage.builder()
                        .responses(bean.getToolResponses())
                        .metadata(bean.getMetadata())
                        .build();
            }
        }
        throw new RuntimeException("Unknown message type: " + messageType);
    }
}
