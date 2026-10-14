package com.yanglizi.level.init;

import cn.hutool.json.JSONUtil;
import com.yanglizi.level.mapper.ApiLevelConfigMapper;
import com.yanglizi.level.mapper.LevelMapper;
import com.yanglizi.level.pojo.dto.LevelCacheDto;
import com.yanglizi.level.pojo.entity.ApiLevelConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.yanglizi.level.constant.LevelConstant.LEVEL_CACHE_RULE_KEY;
import static com.yanglizi.level.constant.LevelConstant.PERMISSION_CONFIG_KEY;

@Slf4j
@Component
@RequiredArgsConstructor
public class LevelCacheInitializer {

    private final LevelMapper levelMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final ApiLevelConfigMapper configMapper;

    /**
     * ⭐ 使用 @PostConstruct 在 Bean 初始化后自动执行
     */
    @PostConstruct
    public void initLevelCache() {
        long startTime = System.currentTimeMillis();
        log.info("========== 开始预热 ==========");

        try {
            levelInit();
            apiLevelConfigInit();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("========== 预热完成，耗时 {}ms ==========", elapsed);

        } catch (Exception e) {
            log.error("预热失败（应用将继续启动，后续将降级查询数据库）", e);
        }
    }

    private void levelInit() {
        // 1. 从数据库查询所有等级（按 min_recipes 升序）
        List<LevelCacheDto> levels = levelMapper.selectAllLevels();
        if (levels == null || levels.isEmpty()) {
            log.warn("等级表为空，跳过预热（请检查数据库）");
            return;
        }

        // 2. 清空旧的缓存（防止脏数据）
        redisTemplate.delete(LEVEL_CACHE_RULE_KEY);

        Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>(levels.size());
        for (LevelCacheDto level : levels) {
            String levelJson = JSONUtil.toJsonStr(level);
            // 创建 TypedTuple：member = JSON 字符串，score = minRecipes
            ZSetOperations.TypedTuple<String> tuple =
                    new DefaultTypedTuple<>(levelJson, (double) level.getRuleMin());

            tuples.add(tuple);
            log.debug("等级准备：code={}, name={}, RuleMin={}",
                    level.getCode(), level.getName(), level.getRuleMin());
        }

        Long addedCount = redisTemplate.opsForZSet().add(LEVEL_CACHE_RULE_KEY, tuples);
        log.info("批量写入 ZSet 成功，共 {} 条记录", addedCount);
    }

    public void apiLevelConfigInit() {
        redisTemplate.delete(PERMISSION_CONFIG_KEY);
        List<ApiLevelConfig> configs = configMapper.selectAll();
        Map<String, String> configMap = configs.stream()
                .collect(Collectors.toMap(
                        ApiLevelConfig::getApiKey,
                        ApiLevelConfig::getLevelCode
                ));
        redisTemplate.opsForHash().putAll(PERMISSION_CONFIG_KEY, configMap);
    }
}