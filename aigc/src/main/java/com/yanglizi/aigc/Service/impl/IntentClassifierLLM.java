package com.yanglizi.aigc.Service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanglizi.aigc.Service.IntentClassifier;
import com.yanglizi.aigc.Service.IntentKeywordCacheService;
import com.yanglizi.aigc.enums.Intent;
import com.yanglizi.aigc.pojo.dto.IntentResult;
import com.yanglizi.aigc.pojo.vo.JevRequest;
import com.yanglizi.aigc.pojo.vo.JevResponse;
import com.yanglizi.common.utils.OkHttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.yanglizi.aigc.constants.ChatConstant.MEMORY_KEY;

@Service
@Slf4j
public class IntentClassifierLLM implements IntentClassifier {

    @Qualifier("intentChatClient")
    @Autowired
    private ChatClient intentChatClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private IntentKeywordCacheService keywordCacheService;

    // ========== 上下文关键词（保留硬编码，与用户输入过滤无关） ==========
    private static final List<String> ASK_START_KEYWORDS = Stream.of(
            "想写日记吗", "开始记录吧", "要写日记吗", "开始写日记吗"
    ).map(String::toLowerCase).collect(Collectors.toList());

    private static final List<String> ASK_DETAIL_KEYWORDS = Stream.of(
            "能说说具体经过吗", "然后呢", "接下来呢", "还有什么细节",
            "具体做了什么", "当时的情况是"
    ).map(String::toLowerCase).collect(Collectors.toList());

    private static final List<String> ASK_GENERATE_KEYWORDS = Stream.of(
            "需要整理成日记吗", "生成日记吗", "要写出来吗"
    ).map(String::toLowerCase).collect(Collectors.toList());

    // ========== 新增：从缓存获取关键词的方法 ==========
    private List<String> getStartKeywords() {
        return keywordCacheService.getKeywords(Intent.START_DIARY.name());
    }

    private List<String> getChatKeywords() {
        return keywordCacheService.getKeywords(Intent.CHAT.name());
    }

    private List<String> getConfirmGenerateKeywords() {
        return keywordCacheService.getKeywords(Intent.CONFIRM_GENERATE.name());
    }

    private List<String> getHelpKeywords() {
        return keywordCacheService.getKeywords(Intent.HELP.name());
    }

    private List<String> getOffTopicKeywords() {
        return keywordCacheService.getKeywords(Intent.OFF_TOPIC.name());
    }

    // ========== 以下代码完全不动 ==========

    public static String INTENT_PROMPT_TEMPLATE;
    static {
        String intentDescriptions = Intent.getAllDescriptions();
        INTENT_PROMPT_TEMPLATE = String.format("""
                你是一个意图识别助手。判断用户输入的意图，返回 JSON 格式：{"intent": "意图值", "confidence": 置信度(0-1)}。
                意图值只能是以下枚举值之一,要严谨判断,不要轻易返回UNKNOWN：
                
                %s
                只输出一个单词，不要解释。
                
                意图：
                """, intentDescriptions);
    }

    // ---------- 公共入口 ----------
    public Intent classify(String sessionId, String userInput, Integer userId) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return Intent.UNKNOWN;
        }
        String text = userInput.trim().toLowerCase();

        String lastAiReply = getLastAiReply(sessionId, userId);

        Intent ruleIntent = classifyByRules(text, lastAiReply);

        if (ruleIntent != Intent.UNKNOWN) {
            return ruleIntent;
        }

        log.info("规则无法判断，触发大模型意图识别兜底: sessionId={}, userId={}, input={}",
                sessionId, userId, userInput);
        Intent llmIntent = classifyByLLM(userInput,lastAiReply );

        log.info("大模型意图识别兜底结果: sessionId={}, userId={}, input={}, ruleResult={}, llmResult={}",
                sessionId, userId, userInput, ruleIntent, llmIntent);

        return llmIntent;
    }

    private Intent classifyByLLM(String userInput, String lastAiReply) {
        /** 调用大模型进行意图识别 */
        try {
            // 获取上下文
            IntentResult result = intentChatClient.prompt()
                    .system(INTENT_PROMPT_TEMPLATE)
                    .user( "上次回复" + lastAiReply + "\n用户输入" + userInput)
                    .options(OpenAiChatOptions.builder()
                            .temperature(0.0))
                    .call()
                    .entity(IntentResult.class);

            if (result != null && result.intent() != null && !result.intent().isEmpty()) {
                try {
                    log.info("大模型意图识别结果: {} 置信度: {}", result.intent(), result.confidence());
                    return Intent.valueOf(result.intent().toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("模型返回了无效的意图值: {}", result.intent());
                    return Intent.UNKNOWN;
                }
            } else {
                return Intent.UNKNOWN;
            }
        } catch (Exception e) {
            log.error("大模型意图识别调用失败: {}", e.getMessage(), e);
            return Intent.UNKNOWN;
        }
    }

    // ---------- 核心分类逻辑 ----------
    private Intent classifyByRules(String text, String lastAiReply) {
        // 1. 明确命令优先（全部改为从缓存读取）
        if (matchesAny(text, getStartKeywords())) {
            return Intent.START_DIARY;
        }
        if (matchesAny(text, getHelpKeywords())) {
            return Intent.HELP;
        }
        if (matchesAny(text, getOffTopicKeywords())) {
            return Intent.OFF_TOPIC;
        }
        if (matchesAny(text, getConfirmGenerateKeywords())) {
            // 还得再判断有没有离题
            return Intent.CONFIRM_GENERATE;

        }

        // 2. 明确闲聊（走规则，不入 LLM）
        if (matchesAny(text, getChatKeywords())) {
            return Intent.CHAT;
        }

        // 3. 简短消息依赖上下文
        if (text.length() <= 5) {
            Intent contextIntent = classifyShortMessage(text, lastAiReply);
            if (contextIntent != Intent.UNKNOWN) {
                return contextIntent;
            }
        }

        // 4. 在日记收集状态
        return Intent.CONTINUE_DIARY;
    }

    // ---------- 简短消息上下文推理 ----------
    private Intent classifyShortMessage(String text, String lastAiReply) {
        if (lastAiReply == null || lastAiReply.isEmpty()) {
            return Intent.UNKNOWN;
        }
        String lowerReply = lastAiReply.toLowerCase();

        boolean affirmative = isAffirmative(text);
        boolean negative = isNegative(text);

        if (matchesAny(lowerReply, ASK_START_KEYWORDS)) {
            if (affirmative) {
                return Intent.START_DIARY;
            }
            if (negative) {
                return Intent.CHAT;
            }
        }

        if (matchesAny(lowerReply, ASK_DETAIL_KEYWORDS)) {
            if (affirmative) {
                return Intent.CONTINUE_DIARY;
            }
            if (negative) {
                return Intent.CONFIRM_GENERATE;
            }
        }

        if (matchesAny(lowerReply, ASK_GENERATE_KEYWORDS)) {
            if (affirmative) {
                return Intent.CONFIRM_GENERATE;
            }
            if (negative) {
                return Intent.CHAT;
            }
        }

        return Intent.UNKNOWN;
    }

    // ---------- 工具方法 ----------
    private boolean matchesAny(String text, List<String> keywords) {
        if (text == null || keywords == null || keywords.isEmpty()) {
            return false;
        }
        String lowerText = text.toLowerCase();
        for (String kw : keywords) {
            if (lowerText.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAffirmative(String text) {
        String t = text.replaceAll("[\\s。！!？?，,~～哦哈呀啦嘞~]", "");
        return t.matches("^(好|好的|嗯|行|可以|没问题|ok|是|对|是的|对的)$");
    }

    private boolean isNegative(String text) {
        String t = text.replaceAll("[\\s。！!？?，,~～哦哈呀啦嘞~]", "");
        return t.matches("^(不|不行|算了|不用|没有|还没|稍等|不是|不对|不对的)$");
    }

    // ---------- Redis 操作 ----------
    private String getLastAiReply(String sessionId, Integer userId) {
        String key = MEMORY_KEY + sessionId + "_" + userId;
        try {
            List<String> range = redisTemplate.opsForList().range(key, -1, -1);
            if (range == null || range.isEmpty()) {
                return null;
            }
            String json = range.getFirst();
            JsonNode node = new ObjectMapper().readTree(json);
            JsonNode textNode = node.get("textContent");
            if (textNode == null) {
                textNode = node.get("content");
            }
            return textNode != null ? textNode.asText() : null;
        } catch (Exception e) {
            log.warn("获取最后一条AI回复的textContent失败: sessionId={}, userId={}", sessionId, userId, e);
            return null;
        }
    }
}