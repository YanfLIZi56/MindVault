package com.yanglizi.config;

import com.yanglizi.aigc.memory.RedisChatMemoryRepository;
import com.yanglizi.diary.tool.DiarySearchTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class AiConfig {

    @Value("${ai.memory.maxMsg:10}")
    public int maxMsg;


    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 Advisor loggerAdvisor,
                                 Advisor redisChatMemoryAdvisor,
                                 DiarySearchTool diarySearchTool) {
            return builder
                .defaultAdvisors(loggerAdvisor, redisChatMemoryAdvisor)
                .defaultTools(diarySearchTool)
                .build();
    }

    @Bean("factChatClient")
    public ChatClient factChatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean("diaryChatClient")
    public ChatClient diaryChatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean("intentChatClient")
    public ChatClient intentChatClient(ChatClient.Builder builder) {
        return builder.build();
    }


    @Bean
    public Advisor loggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }

    @Bean
    public ChatMemory redisChatMemory(ChatMemoryRepository redisChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisChatMemoryRepository)
                .maxMessages(maxMsg)
                .build();
    }

    @Bean
    public ChatMemoryRepository redisChatMemoryRepository() {
        return new RedisChatMemoryRepository();
    }

    @Bean
    public Advisor redisChatMemoryAdvisor(ChatMemory redisChatMemory) {
        return MessageChatMemoryAdvisor.builder(redisChatMemory)
                .build();
    }
}
