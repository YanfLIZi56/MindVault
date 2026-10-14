package com.yanglizi.aigc.Service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanglizi.aigc.Service.IntentClassifier;
import com.yanglizi.aigc.enums.Intent;
import com.yanglizi.aigc.pojo.vo.JevRequest;
import com.yanglizi.aigc.pojo.vo.JevResponse;
import com.yanglizi.common.utils.OkHttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yanglizi.aigc.constants.ChatConstant.MEMORY_KEY;

@Service
@Slf4j
public class IntentClassifierJev implements IntentClassifier {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${mindVault.jev.api.key}")
    private String API_KEY;
    @Value("${mindVault.jev.api.url}")
    private String JEV_URL;
    @Value("${mindVault.jev.model:jev-latest}")
    private String JEV_MODEL;
    @Value("${mindVault.jev.minConfidence:0.5}")
    private double minConfidence;

    // ---------- 公共入口 ----------
    public Intent classify(String sessionId, String userInput, Integer userId) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return Intent.UNKNOWN;
        }

        String lastAiReply = getLastAiReply(sessionId, userId);

        Intent jevIntent = classifyByJev(userInput,lastAiReply );

        log.info("大模型意图识别兜底结果: sessionId={}, userId={}, input={}, jevResult={}",
                sessionId, userId, userInput, jevIntent);

        return jevIntent;
    }

    private Intent classifyByJev(String userInput, String lastAiReply) {
        /* 调用jev模型进行意图识别 */
        // 1.组装header
        Map<String,String> headers = new HashMap<>();
        headers.put("Authorization","Bearer " + API_KEY);

        // 2.组装criteria选项
        Map<String,String> criteriaMap = new HashMap<>();
        criteriaMap.put("START_DIARY","用户想开始写日记");
        criteriaMap.put("CONTINUE_DIARY","用户正在提供日记内容/细节");
        criteriaMap.put("CONFIRM_GENERATE","用户确认想生成日记");
        criteriaMap.put("CHAT","普通闲聊（打招呼、问好、日常聊天）");
        criteriaMap.put("HELP","询问功能（'你能做什么'、'怎么用'）");
        criteriaMap.put("OFF_TOPIC","明显偏题，跟你聊与日记/聊天无关（如'帮我写代码'、'今天股票怎么样'）");
        criteriaMap.put("UNKNOWN","无法判断");

        // 3.构造question对象
        JevRequest.QuestionItem intentQuestion = new JevRequest.QuestionItem();
        intentQuestion.setType("choice");
        intentQuestion.setInstructions("这是一个帮助用户写日记的应用，需要判断用户意图");
        intentQuestion.setCriteria(criteriaMap);

        Map<String,JevRequest.QuestionItem> questionsMap = new HashMap<>();
        questionsMap.put("intent", intentQuestion);

        // 4.state拼接，严格按照示例格式
        String stateVal = "[上一轮的ai回答]\n" + lastAiReply + "\n\n[用户输入]\n" + userInput;

        // 5.组装完整请求体
        JevRequest jevReq = new JevRequest();
        jevReq.setModel(JEV_MODEL);
        jevReq.setQuestions(questionsMap);
        jevReq.setState(stateVal);

        // 6.发起http调用（静态工具类，自动序列化、反序列化为JevResponse）
        JevResponse resp = OkHttpUtil.postJsonSafe(JEV_URL, jevReq, headers, JevResponse.class);
        // 请求失败：网络异常、http非200、json解析异常都会走到这里，记得打error日志
        if (resp == null) {
            log.error("调用jev模型失败: {}", jevReq);
            return Intent.UNKNOWN;
        }

        String intent = resp.getAnswers().getIntent().getChoice();
        Double confidence = resp.getAnswers().getIntent().getConfidence();
        log.info("jev模型意图识别结果: {}", resp);
        if (confidence < minConfidence) {
            return Intent.UNKNOWN;
        }
        return Intent.valueOf(intent.toUpperCase());
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
