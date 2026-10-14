package com.yanglizi.diary.service.impl;

import cn.hutool.core.lang.Snowflake;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yanglizi.aigc.exception.AiException;
import com.yanglizi.aigc.mapper.ChatMapper;
import com.yanglizi.diary.exception.DiaryException;
import com.yanglizi.diary.mapper.DiaryMapper;
import com.yanglizi.diary.pojo.dto.DiaryDto;
import com.yanglizi.diary.pojo.dto.QueryDiaryDto;
import com.yanglizi.diary.pojo.entry.Diary;
import com.yanglizi.diary.pojo.vo.DiaryIdVo;
import com.yanglizi.diary.pojo.vo.DiaryListVo;
import com.yanglizi.diary.pojo.vo.DiaryVo;
import com.yanglizi.diary.service.DiaryService;
import com.yanglizi.diary.service.EmotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import static com.yanglizi.aigc.constants.GenStatusContants.*;

@Service
public class DiaryServiceImpl implements DiaryService {

    public static final String DIARY_VERSION_KEY = "diary:version:";
    @Autowired
    private ChatMapper chatMapper;
    @Autowired
    private Snowflake snowflake;
    @Autowired
    private DiaryMapper diaryMapper;
    @Autowired
    private EmotionService emotionService;
    @Autowired
    private AsyncDiaryServiceImpl asyncDiaryService;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${mindVault.diary.emotionSize:10}")
    public int emotionSize = 10;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiaryIdVo genDiary(String sessionId, Integer userId) {
        int i = chatMapper.checkSession(sessionId, userId);
        if (i == 0) {
            throw new AiException("无效会话");
        }
        String versionGroup = chatMapper.selectVersionGroup(sessionId, userId);
        if (versionGroup == null){
            versionGroup = snowflake.nextIdStr();
        }
        Long version = redisTemplate.opsForValue().increment(DIARY_VERSION_KEY + versionGroup,1);

        Diary diary = Diary.builder()
                .userId(userId)
                .status(GENERATING)
                .versionGroup(versionGroup)
                .isCurrent(0)
                .version(version.intValue())
                .sourceType(1)
                .build();
        int insert = diaryMapper.insert(diary);
        chatMapper.updateVersionGroup(sessionId, userId, versionGroup);
        if (insert != 1) {
            throw new DiaryException("生成日记失败");
        }
        asyncDiaryService.generateDiary(sessionId, diary.getId());

        return DiaryIdVo.builder()
                .sessionId(sessionId)
                .diaryId(diary.getId())
                .build();
    }

    @Override
    public DiaryVo getDiaryById(Integer id, Integer userId) {
        return diaryMapper.getDiaryById(id, userId);
    }

    @Override
    public List<Diary> getDiaryListBySessionId(Integer userId, String versionGroup) {
        if (versionGroup == null || versionGroup.isEmpty()){
            return null;
        }
        return diaryMapper.getDiaryListByVersionGroup(userId, versionGroup);
    }

    @Override
    public DiaryListVo getDiaryList(QueryDiaryDto dto, Integer userId) {
        dto.setUserId(userId);
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
        List<DiaryVo> diaryList = diaryMapper.getDiaryList(dto);
        PageInfo<DiaryVo> pageInfo = new PageInfo<>(diaryList);
        return new DiaryListVo(pageInfo.getTotal(), pageInfo.getList());
    }

    @Override
    public DiaryListVo getDiaryQueue(QueryDiaryDto dto, Integer userId) {
        dto.setUserId(userId);
        // 要确保status有值
        if (dto.getStatus() == null){
            throw new DiaryException("状态不能为空");
        }
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
        List<DiaryVo> diaryList = diaryMapper.getDiaryListByStatus(dto);
        PageInfo<DiaryVo> pageInfo = new PageInfo<>(diaryList);
        return new DiaryListVo(pageInfo.getTotal(), pageInfo.getList());
    }

    @Override
    public int confirmDiary(DiaryDto dto, Integer userId) {
        handleEmotions(dto);
        int status = diaryMapper.getStatusById(dto.getId(), dto.getVersionGroup(), userId);
        if (status != SUCCESS) {
            throw new DiaryException("日记未完成噢~");
        }
        diaryMapper.closeCurrent(dto.getVersionGroup(), userId);
        Diary diary = new Diary();
        diary.setId(dto.getId());
        diary.setDiaryDate(dto.getDiaryDate());
        diary.setEmotions(dto.getEmotions());
        diary.setContent(dto.getContent());
        diary.setIsCurrent(1);
        diary.setConfirmedAt(LocalDateTime.now());

        return diaryMapper.update(diary);
    }

    @Override
    public int submitNewVersionDiary(DiaryDto dto, Integer userId) {
        handleEmotions(dto);
        if (dto.getVersionGroup() == null){
            throw new DiaryException("版本组不能为空");
        }
        Long version = redisTemplate.opsForValue().increment(DIARY_VERSION_KEY + dto.getVersionGroup(),1);

        diaryMapper.closeCurrent(dto.getVersionGroup(), userId);
        Diary diary = Diary.builder()
                .userId(userId)
                .emotions(dto.getEmotions())
                .content(dto.getContent())
                .diaryDate(dto.getDiaryDate())
                .status(MANUAL)
                .versionGroup(dto.getVersionGroup())
                .isCurrent(1)
                .version(version.intValue())
                .sourceType(2)
                .confirmedAt(LocalDateTime.now())
                .build();
        return diaryMapper.insert(diary);
    }

    private void handleEmotions(DiaryDto dto) {
        List<String> emotionTags = emotionService.getEmotionTags();
        List<String> emotions = dto.getEmotions();
        if (emotions != null) {
            if (emotions.size() > emotionSize){
                throw new DiaryException("情感标签数量不能超过" + emotionSize + "个");
            }
            List<String> newEmotions = new LinkedList<>();
            for (String emotion : emotions) {
                if (!emotionTags.contains(emotion)) {
                    emotion = emotion.replace("custom:", "");
                    emotion = "custom:" + emotion;
                }
                newEmotions.add(emotion);
            }
            dto.setEmotions(newEmotions);
        }
    }

}
