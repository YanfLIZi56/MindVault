package com.yanglizi.level.Service.impl;

import cn.hutool.json.JSONUtil;
import com.yanglizi.level.Service.LevelService;
import com.yanglizi.level.exception.LevelException;
import com.yanglizi.level.mapper.LevelMapper;
import com.yanglizi.level.pojo.dto.LevelCacheDto;
import com.yanglizi.user.Service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

import static com.yanglizi.level.constant.LevelConstant.LEVEL_CACHE_RULE_KEY;

@Service
@Slf4j
public class LevelServiceImpl implements LevelService {

    @Autowired
    private LevelMapper levelMapper;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Override
    public LevelCacheDto getLevelByRule(int i) {
        // member: "{"id":1, "code":2, "name":"入门","iconUrl":"https://...", "ruleMin":5}"
        // scope: ruleMin
        Set<String> results = redisTemplate.opsForZSet()
                .reverseRangeByScore(LEVEL_CACHE_RULE_KEY, 0, i, 0, 1);

        if (results != null && !results.isEmpty()) {
            String levelJson = results.iterator().next();
            log.debug("从缓存中获取等级信息：{}", levelJson);
            return JSONUtil.toBean(levelJson, LevelCacheDto.class);
        }

        LevelCacheDto level = levelMapper.getLevelByRule(i);
        if (level != null) {
            redisTemplate.opsForZSet().add(LEVEL_CACHE_RULE_KEY, JSONUtil.toJsonStr(level), level.getRuleMin());
        }
        return level;
    }

    @Override
    public Integer getLevelByCode(Integer levelId) {
        return levelMapper.getLevelByCode(levelId);
    }


}
