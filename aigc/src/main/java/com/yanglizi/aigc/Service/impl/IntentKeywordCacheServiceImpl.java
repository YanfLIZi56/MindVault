package com.yanglizi.aigc.Service.impl;

import com.yanglizi.aigc.Service.IntentKeywordCacheService;
import com.yanglizi.aigc.enums.Intent;
import com.yanglizi.aigc.mapper.IntentKeywordMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class IntentKeywordCacheServiceImpl implements IntentKeywordCacheService {

    private final IntentKeywordMapper keywordMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_KEY_PREFIX = "intent:keywords:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    // 所有支持的意图类型
    private static final List<String> INTENT_TYPES = List.of(
            Intent.START_DIARY.name(),
            Intent.CONFIRM_GENERATE.name(),
            Intent.CHAT.name(),
            Intent.HELP.name(),
            Intent.OFF_TOPIC.name()
    );

    /**
     * 启动时加载所有关键词到 Redis
     */
    @PostConstruct
    public void init() {
        log.info("初始化意图关键词缓存");
        refreshAll();
    }

    /**
     * 刷新所有类型的关键词缓存
     */
    @Override
    public void refreshAll() {
        for (String type : INTENT_TYPES) {
            refreshByType(type);
        }
        log.info("所有意图关键词缓存已刷新");
    }

    /**
     * 刷新指定类型的关键词缓存
     */
    @Override
    public void refreshByType(String intentType) {
        try {
            List<String> keywords = keywordMapper.selectKeywordsByType(intentType);
            // 转小写
            List<String> lowerKeywords = keywords.stream()
                    .map(String::toLowerCase)
                    .toList();

            String key = REDIS_KEY_PREFIX + intentType;
            // 使用 List 存储，先删除旧列表再重新添加
            redisTemplate.delete(key);
            if (!lowerKeywords.isEmpty()) {
                ListOperations<String, Object> listOps = redisTemplate.opsForList();
                // 批量右推
                listOps.rightPushAll(key, lowerKeywords.toArray());
                redisTemplate.expire(key, CACHE_TTL);
            }
            log.debug("关键词缓存刷新: {} -> {} 个词", intentType, lowerKeywords.size());
        } catch (Exception e) {
            log.error("刷新关键词缓存失败: {}", intentType, e);
        }
    }

    /**
     * 获取指定类型的关键词列表（小写）
     */
    @Override
    public List<String> getKeywords(String intentType) {
        String key = REDIS_KEY_PREFIX + intentType;
        ListOperations<String, Object> listOps = redisTemplate.opsForList();
        List<Object> range = listOps.range(key, 0, -1);
        if (range == null || range.isEmpty()) {
            // 缓存不存在或已过期，主动从数据库加载并回填
            refreshByType(intentType);
            range = listOps.range(key, 0, -1);
        }
        return range == null ? Collections.emptyList() :
                range.stream().map(Object::toString).collect(Collectors.toList());
    }

    /**
     * 获取所有类型的关键词 Map
     */
    @Override
    public Map<String, List<String>> getAllKeywords() {
        Map<String, List<String>> result = new java.util.HashMap<>();
        for (String type : INTENT_TYPES) {
            result.put(type, getKeywords(type));
        }
        return result;
    }

    /**
     * 清除所有缓存
     */
    @Override
    public void clearAll() {
        for (String type : INTENT_TYPES) {
            redisTemplate.delete(REDIS_KEY_PREFIX + type);
        }
        log.info("所有意图关键词缓存已清除");
    }

    /**
     * 获取缓存中的关键词数量（用于监控）
     */
    @Override
    public long getTotalKeywordCount() {
        long count = 0;
        for (String type : INTENT_TYPES) {
            String key = REDIS_KEY_PREFIX + type;
            Long size = redisTemplate.opsForList().size(key);
            if (size != null) {
                count += size;
            }
        }
        return count;
    }
}