package com.yanglizi.diary.service.impl;

import com.yanglizi.diary.mapper.EmotionMapper;
import com.yanglizi.diary.pojo.entry.Emotion;
import com.yanglizi.diary.service.EmotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class EmotionServiceImpl implements EmotionService {
    @Autowired
    private EmotionMapper emotionMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    private static final String EMOTIONS_KEY = "emotions";


    @Override
    public List<Emotion> getEmotions() {
        List<Emotion> emotions = (List<Emotion>) redisTemplate.opsForValue().get(EMOTIONS_KEY);
        if (emotions != null) {
            return emotions;
        }
        emotions = emotionMapper.selectEmotion();
        redisTemplate.opsForValue().set(EMOTIONS_KEY, emotions, Duration.ofHours(1));
        return emotions;

    }

    @Override
    public List<String> getEmotionTags() {
        List<Emotion> emotions = getEmotions();
        return emotions.stream().map(Emotion::getTag).toList();
    }
}
