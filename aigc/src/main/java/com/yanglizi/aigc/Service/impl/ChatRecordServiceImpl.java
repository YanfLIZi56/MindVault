package com.yanglizi.aigc.Service.impl;

import com.yanglizi.aigc.exception.AiException;
import com.yanglizi.aigc.mapper.ChatMapper;
import com.yanglizi.aigc.pojo.entry.DiaryConversations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.yanglizi.aigc.constants.ChatConstant.AI_SPEAKER;
import static com.yanglizi.aigc.constants.ChatConstant.USER_SPEAKER;

@Service
public class ChatRecordServiceImpl {

    @Autowired
    private ChatMapper chatMapper;

    @Transactional(rollbackFor = Exception.class)
    public Integer saveQuestion(String sessionId, String question, Integer userId) {
        LocalDateTime now = LocalDateTime.now();
        Integer round = chatMapper.getNewRound(sessionId, userId, now);
        if (round == null) {
            throw new AiException("无效会话");
        }
        DiaryConversations build = DiaryConversations.builder()
                .sessionId(sessionId)
                .round(round)
                .speaker(USER_SPEAKER)
                .content(question)
                .build();
        chatMapper.saveConversation(build);
        return round;

    }

    @Transactional(rollbackFor = Exception.class)
    public void saveAnswer(String sessionId, String aiText, Integer round) {
        DiaryConversations build = DiaryConversations.builder()
                .sessionId(sessionId)
                .round(round)
                .speaker(AI_SPEAKER)
                .content(aiText)
                .build();
        chatMapper.saveConversation(build);
    }
}
