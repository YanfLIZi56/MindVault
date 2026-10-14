package com.yanglizi.aigc.Service.impl;

import cn.hutool.json.JSONUtil;
import com.yanglizi.aigc.mapper.ChatMapper;
import com.yanglizi.aigc.pojo.dto.ConversationDto;
import com.yanglizi.aigc.pojo.entry.ChatSession;
import com.yanglizi.aigc.pojo.entry.DiaryConversations;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.yanglizi.aigc.constants.ChatConstant.*;

@Slf4j
@Component
public class AsyncChatServiceImpl {

    @Autowired
    private ChatMapper chatMapper;

    @Qualifier("factChatClient")
    @Autowired
    private ChatClient chatClient;

    @Value("${ai.memory.maxMsg:10}")
    private Integer maxRound;
    @Value("${ai.genRawFactsPrompt}")
    private String genRawFactsPrompt;

    @Async
    public void generateRawFacts(String sessionId, Integer round) {
        int window = maxRound / 2;
        ChatSession chatSession = chatMapper.selectRawFactsLastRound(sessionId);
        if (chatSession == null) {
            return;
        }

        int diff = round - chatSession.getRawFactsLastRound();
        if (diff < window) {
            return;
        }

        // 乐观锁一下, 防止重复处理
        int lockAffected = chatMapper.tryLockRawFactsRound(sessionId, round);
        if (lockAffected <= 0) {
            log.info("该轮摘要已处理/已有更新的摘要，跳过 session={}, round={}", sessionId, round);
            return;
        }

        String newRawFacts = getNewRawFacts(sessionId, chatSession);
        // 摘要 保存起来
        chatMapper.generateRawFacts(sessionId, newRawFacts, round);

        /*
            定时任务做补偿:
            select * from chat_sessions where raw_facts_pending_round is not null;
        */
    }

    @Nullable
    public String getNewRawFacts(String sessionId, ChatSession chatSession) {
        String oldRawFacts = chatSession.getRawFacts();

        // > RawFactsLastRound 距离上次摘要的最新的对话
        List<DiaryConversations> conversations = chatMapper.selectConversationByRoundRange(sessionId, chatSession.getRawFactsLastRound());
        // 翻转 保证顺序
        Collections.reverse(conversations);

        List<ConversationDto> conversationDtos = new ArrayList<>();
        for (DiaryConversations conversation : conversations){
            String role;
            if (conversation.getSpeaker().equals(USER_SPEAKER)) {
                role = ROLE_USER;
            } else if (conversation.getSpeaker().equals(AI_SPEAKER)) {
                role = ROLE_AI;
            }else {
                continue;
            }
            conversationDtos.add(ConversationDto.builder()
                    .round(conversation.getRound())
                    .content(conversation.getContent())
                    .role(role)
                    .build());
        }
        // 把对话列表变成json
        String conversationsJson = JSONUtil.toJsonStr(conversationDtos);

        // 生成摘要
        // 首次摘要oldRawFacts为空
        if (oldRawFacts == null || oldRawFacts.isBlank()) {
            oldRawFacts = "无";
        }

        String finalOldRawFacts = oldRawFacts;
        String renderedPrompt = genRawFactsPrompt
                .replace("{ old_summary }", finalOldRawFacts)
                .replace("{ new_conversations }", conversationsJson);

        String newRawFacts = chatClient
                .prompt(renderedPrompt)
                .call()
                .content();
        log.info("生成的摘要: {}", newRawFacts);
        return newRawFacts;
    }

    /**
     * 异步生成标题
     */
    @Async
    public void generateTitle(String sessionId,String question, Integer userId) {
        // 判断有没有标题
        String title = chatMapper.hasTitle(sessionId, userId);
        if (title == null || title.isEmpty()) {
            // 生成标题
            String newTitle = questionForTitle(question);
            chatMapper.saveTitle(sessionId, newTitle, userId, LocalDateTime.now());
            log.info("生成标题: {}", newTitle);
        }
    }

    private String questionForTitle(String question) {
        return question.substring(0, Math.min(question.length(), 100));
    }
}
