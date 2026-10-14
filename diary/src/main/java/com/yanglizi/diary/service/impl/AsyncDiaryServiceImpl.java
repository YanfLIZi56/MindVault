package com.yanglizi.diary.service.impl;

import com.yanglizi.diary.service.AsyncVectorService;
import com.yanglizi.aigc.Service.impl.AsyncChatServiceImpl;
import com.yanglizi.aigc.exception.AiException;
import com.yanglizi.aigc.mapper.ChatMapper;
import com.yanglizi.aigc.pojo.entry.ChatSession;
import com.yanglizi.diary.mapper.DiaryMapper;
import com.yanglizi.diary.mapper.EmotionMapper;
import com.yanglizi.diary.pojo.dto.genDiaryDto;
import com.yanglizi.diary.pojo.entry.Diary;
import com.yanglizi.diary.service.EmotionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Slf4j
@Component
public class AsyncDiaryServiceImpl {

    @Autowired
    private ChatMapper chatMapper;
    @Qualifier("diaryChatClient")
    @Autowired
    private ChatClient chatClient;
    @Autowired
    private AsyncChatServiceImpl asyncChatService;
    @Autowired
    private DiaryMapper diaryMapper;
    @Autowired
    private AsyncVectorService asyncVectorService;
    @Autowired
    private EmotionService emotionService;

    @Value("${ai.genDiaryPrompt}")
    private String genDiaryPrompt;

    @Async
    @Transactional(rollbackFor = Exception.class)
    public void generateDiary(String sessionId, Integer diaryId) {
        log.info("sessionId: {} 生成日记: {}", sessionId, diaryId);

        ChatSession chatSession = chatMapper.selectSessionById(sessionId);
        String newRawFacts = chatSession.getRawFacts();

        // 检查摘要是不是最新的
        if (chatSession.getRawFactsLastRound() != null && chatSession.getLastRound() != null && chatSession.getRawFactsLastRound() < chatSession.getLastRound()) {
            log.debug("摘要不是最新的，不生成日记: 当前摘要轮数: {}, 最新轮数: {}",
                    chatSession.getRawFactsLastRound(), chatSession.getLastRound());
            // 乐观锁一下，防止重复处理
            int lockAffected = chatMapper.tryLockRawFactsRound(sessionId, chatSession.getLastRound());
            if (lockAffected <= 0) {
                log.debug("该轮摘要已处理/已有更新的摘要，跳过 session={}, round={}",
                        sessionId, chatSession.getLastRound());
                return;
            }
            newRawFacts = asyncChatService.getNewRawFacts(sessionId, chatSession);
            // 摘要保存起来
            chatMapper.generateRawFacts(sessionId, newRawFacts, chatSession.getLastRound());
        }

        // 生成日记, 调用工具, 查询心情
        if (newRawFacts == null || newRawFacts.isEmpty()) {
            log.warn("rawFacts 为空，无法生成日记: diaryId={}", diaryId);
            return;
        }

        // 查询已有心情
        List<String> emotions = emotionService.getEmotionTags();
        String emotion = emotions != null && !emotions.isEmpty() ? String.join(", ", emotions) : "未提供";

        // 渲染 Prompt
        String renderedPrompt = genDiaryPrompt
                .replace("{ emotions }", emotion)
                .replace("{ raw_facts }", newRawFacts);

        genDiaryDto aiRes;
        try {
            aiRes = chatClient.prompt(renderedPrompt)
                            .call()
                            .entity(genDiaryDto.class);
        } catch (Exception e) {
            log.warn("AI 生成日记失败: sessionId={}, diaryId={}", sessionId, diaryId, e);
            Diary diary = Diary.builder()
                    .id(diaryId)
                    .status(4) // 失败
                    .build();
            diaryMapper.update(diary);
            throw new AiException("AI 生成日记失败");
        }

        String content;
        if (aiRes == null) {
            log.warn("AI 生成的日记为空: sessionId={}, diaryId={}", sessionId, diaryId);
            Diary diary = Diary.builder()
                    .id(diaryId)
                    .status(4) // 失败
                    .build();
            diaryMapper.update(diary);
            throw new AiException("AI 生成的日记为空");
        }
        content = aiRes.getContent();
        if (content == null || content.isEmpty()){
            log.warn("AI 生成的日记内容为空: sessionId={}, diaryId={}", sessionId, diaryId);
            content = "AI 生成的日记内容为空";
        }

        Diary diary = Diary.builder()
                .id(diaryId)
                .content(content)
                .emotions(aiRes.getEmotions())
                .emotionScore(aiRes.getEmotionScore())
                .status(3) // 完成
                .build();
        diaryMapper.update(diary);

        // 这里插入向量库
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        // 事务已提交，安全触发异步向量化
                        asyncVectorService.addDiaryVector(diaryId);
                    }
                }
        );
    }
}
