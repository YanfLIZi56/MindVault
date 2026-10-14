package com.yanglizi.diary.service;

import com.yanglizi.diary.pojo.entry.Emotion;

import java.util.List;

public interface EmotionService {
    List<Emotion> getEmotions();

    List<String> getEmotionTags();
}
