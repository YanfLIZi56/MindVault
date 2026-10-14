package com.yanglizi.aigc.Service.impl;

import com.yanglizi.aigc.Service.ChatService;
import com.yanglizi.aigc.Service.IntentClassifier;
import com.yanglizi.aigc.enums.EventType;
import com.yanglizi.aigc.enums.Intent;
import com.yanglizi.aigc.exception.AiException;
import com.yanglizi.aigc.mapper.ChatMapper;
import com.yanglizi.aigc.pojo.dto.ChatPayload;
import com.yanglizi.aigc.pojo.dto.ChunkChatPayload;
import com.yanglizi.aigc.pojo.dto.DoneChatPayload;
import com.yanglizi.aigc.pojo.dto.ErrorChatPayload;
import com.yanglizi.aigc.pojo.entry.DiaryConversations;
import com.yanglizi.aigc.pojo.vo.ChatHistoryCursorVO;
import com.yanglizi.common.pojo.dto.CursorDto;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.yanglizi.aigc.constants.ChatConstant.*;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    public static final int CHAT_ACTIVE = 5;
    @Autowired
    private ChatMapper chatMapper;
    @Autowired
    private ChatClient chatClient;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private ChatRecordServiceImpl chatRecordService;
    @Autowired
    private AsyncChatServiceImpl asyncChatService;
    @Autowired
    @Qualifier("intentClassifierJev")
    private IntentClassifier intentClassifier;

    @Value("${ai.systemPrompt}")
    private String systemPrompt;
    @Value("${ai.streamEnabled}")
    private boolean streamEnabled;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


    @Override
    public Flux<ServerSentEvent<Object>> chat(String question, String sessionId, Integer userId) {
        return Mono.fromCallable(() -> {
                    if (chatMapper.checkSession(sessionId, userId) != 1) {
                        throw new AiException("无效会话");
                    }
                    return true;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(ignored -> {
                    // 1. 保存用户问题，获取 round
                    Mono<Integer> saveUserQuestionMono = Mono.fromCallable(() -> {
                        asyncChatService.generateTitle(sessionId, question, userId);
                        return chatRecordService.saveQuestion(sessionId, question, userId);
                    }).subscribeOn(Schedulers.boundedElastic());

                    return saveUserQuestionMono
                            .flatMapMany(round -> {
                                // 2. 意图识别
                                Intent intent = intentClassifier.classify(sessionId, question, userId);

                                // 3. 根据意图分流
                                if (intent == Intent.START_DIARY) {
                                    String reply = "好的，让我们一起记录今天的经历吧。今天有什么特别的事情吗？";
                                    return buildPredefinedReplyFlux(reply, round, sessionId,Intent.START_DIARY);
                                }
                                else if (intent == Intent.OFF_TOPIC) {
                                    String reply = "我主要陪您聊天并记录日记哦，请说说您今天想记的事情吧～";
                                    return buildPredefinedReplyFlux(reply, round, sessionId, Intent.OFF_TOPIC);
                                }
                                else if (intent == Intent.HELP) {
                                    String reply = "您可以这样说：'我想写日记'，然后我会一步步引导您回忆今天的经历。也可以直接说'帮我总结'来生成日记。";
                                    return buildPredefinedReplyFlux(reply, round, sessionId, Intent.HELP);
                                }
                                else if (intent == Intent.CONFIRM_GENERATE) {
                                    // 先返回预置消息，生成在后台执行
                                    // 前端接收到该意图的回答再调用生成日记接口
                                    String reply = "好的，已在为您生成日记...";
                                    return buildPredefinedReplyFlux(reply, round, sessionId, Intent.CONFIRM_GENERATE);
                                }
                                else {
                                    // 4. 其他意图（CHAT / CONTINUE_DIARY / UNKNOWN）走 AI 流
                                    return getAiAnswer(question, sessionId, userId, round);
                                }
                            });
                })
                .onErrorResume(AiException.class, e ->
                        Flux.just(getSseMsg(EventType.error,
                                ErrorChatPayload.builder().message(e.getMessage()).code(500).build()))
                )
                .onErrorResume(e -> {
                    log.error("AI生成错误: sessionId={}", sessionId, e);
                    return Flux.just(getSseMsg(EventType.error,
                            ErrorChatPayload.builder().message("服务内部错误").code(500).build()));
                });
    }

    @NotNull
    private Flux<ServerSentEvent<Object>> getAiAnswer(String question, String sessionId, Integer userId, Integer round) {
        String rawFacts = chatMapper.getRawFacts(sessionId, userId);
        if (rawFacts == null) {
            rawFacts = "";
        }
        String finalRawFacts = rawFacts;

        // 构建 AI 请求
        var promptSpec = chatClient.prompt()
                .system(spec -> spec.text(systemPrompt)
                        .params(Map.of("now", LocalDateTime.now().format(FORMATTER),
                                "rawFacts", finalRawFacts
                        )))
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, sessionId + "_" + userId))
                .user(question);

        Flux<ChatResponse> aiResponseFlux;
        if (streamEnabled) {
            aiResponseFlux = promptSpec.stream().chatResponse();
        } else {
            aiResponseFlux = Mono.fromCallable(() -> promptSpec.call().chatResponse())
                    .subscribeOn(Schedulers.boundedElastic())
                    .flux();
        }

        AtomicReference<StringBuilder> aiBuffer = new AtomicReference<>(new StringBuilder());

        return aiResponseFlux
                .doOnSubscribe(s -> {
                    redisTemplate.opsForValue().set(SESSION_KEY + sessionId, "1",
                            Duration.ofMinutes(CHAT_ACTIVE));
                })
                .publishOn(Schedulers.boundedElastic())
                .takeWhile(resp -> Boolean.TRUE.equals(redisTemplate.hasKey(SESSION_KEY + sessionId)))
                .filter(resp -> resp.getResult() != null
                        && resp.getResult().getOutput().getText() != null
                        && !resp.getResult().getOutput().getText().isEmpty())
                .map(resp -> {
                    String text = resp.getResult().getOutput().getText();
                    aiBuffer.get().append(text);
                    return getSseMsg(EventType.chunk,
                            ChunkChatPayload.builder().delta(text).round(round).build());
                })
                .doFinally(signalType -> {
                    String fullText = aiBuffer.get().toString();
                    redisTemplate.delete(SESSION_KEY + sessionId);

                    // 判断流终止类型
                    String persistAiText;
                    if (signalType == SignalType.ON_ERROR) {
                        persistAiText = null;
                    } else {
                        persistAiText = fullText;
                    }

                    Mono.fromCallable(() -> {
                                chatRecordService.saveAnswer(sessionId, persistAiText, round);
                                return true;
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .onErrorResume(e -> {
                                log.error("保存AI回答事务失败 sessionId={}, signalType={}",
                                        sessionId, signalType, e);
                                return Mono.empty();
                            })
                            .subscribe(
                                    unused -> {
                                        if (persistAiText != null) {
                                            asyncChatService.generateRawFacts(sessionId, round);
                                        }
                                    },
                                    err -> log.error("AI入库订阅层异常", err)
                            );
                })
                .concatWith(Flux.just(getSseMsg(EventType.done, null)));
    }

    @NotNull
    private static ServerSentEvent<Object> getSseMsg(EventType eventType, ChatPayload data) {
        return ServerSentEvent.builder()
                .data(data)
                .event(eventType.name())
                .build();
    }

    /**
     * 构建预置回复的 SSE 流（无需调用大模型）
     * @param reply 预置回复内容
     * @param round 用户消息的轮次（用于关联）
     * @param sessionId 会话ID
     * @return SSE Flux
     */
    private Flux<ServerSentEvent<Object>> buildPredefinedReplyFlux(String reply, Integer round, String sessionId, Intent action) {
        return Flux.just(
                        getSseMsg(EventType.chunk, ChunkChatPayload.builder().delta(reply).round(round).build())
                ).concatWith(Flux.just(getSseMsg(EventType.done,
                        DoneChatPayload.builder()
                        .action(action.ordinal())
                        .build()))
                )
                .doFinally(signalType -> {
                    // 保存 AI 回答（预置回复）
                    Mono.fromCallable(() -> {
                                chatRecordService.saveAnswer(sessionId, reply, round);
                                return true;
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe(
                                    unused -> log.debug("预置回复落库成功: sessionId={}", sessionId),
                                    err -> log.error("保存预置AI回答失败 sessionId={}", sessionId, err)
                            );
                });
    }

    @Override
    public void stop(String sessionId, Integer userId) {
        if (chatMapper.checkSession(sessionId, userId) != 1) {
            throw new AiException("无效会话");
        }
        redisTemplate.delete(SESSION_KEY+sessionId);
    }

    @Override
    public ChatHistoryCursorVO<DiaryConversations> getChatHistory(String sessionId, Integer userId, CursorDto dto) {
        if (chatMapper.checkSession(sessionId, userId) != 1) {
            throw new AiException("无效会话");
        }
        Integer cursor = dto.getCursor();
        int roundSize = dto.getRoundSize() * 2;
        List<DiaryConversations> list = chatMapper.getChatHistory(sessionId, cursor, roundSize +1);
        boolean hasMore = list.size() > roundSize;
        if(hasMore){
            //丢弃多查出来的那一条
            list = list.subList(0, roundSize);
        }
        return ChatHistoryCursorVO.<DiaryConversations>builder()
                .records(list)
                .hasMore(hasMore)
                .build();
    }

    @Override
    public String testChat(String question, String test) {
        String systemPrompt = """
            你是 MindVault，一个温暖的日记助手。
            当用户表达低落情绪时，调用 searchDiaries 工具搜索开心的日记。
            """;

        return chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, test + "_" + -1))
                .call()
                .content();

    }

}
