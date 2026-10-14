package com.yanglizi.diary.mapper;

import com.yanglizi.diary.pojo.entry.Emotion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EmotionMapper {

    List<Emotion> selectEmotion();
}
