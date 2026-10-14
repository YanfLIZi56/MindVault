package com.yanglizi.diary.service;

import com.yanglizi.diary.pojo.dto.DiaryDto;
import com.yanglizi.diary.pojo.dto.QueryDiaryDto;
import com.yanglizi.diary.pojo.entry.Diary;
import com.yanglizi.diary.pojo.vo.DiaryIdVo;
import com.yanglizi.diary.pojo.vo.DiaryListVo;
import com.yanglizi.diary.pojo.vo.DiaryVo;

import java.util.List;

public interface DiaryService {
    DiaryIdVo genDiary(String sessionId, Integer userId);

    DiaryVo getDiaryById(Integer id, Integer userId);

    List<Diary> getDiaryListBySessionId(Integer userId, String versionGroup);

    DiaryListVo getDiaryList(QueryDiaryDto dto, Integer userId);

    DiaryListVo getDiaryQueue(QueryDiaryDto dto, Integer userId);

    int confirmDiary(DiaryDto dto, Integer userId);

    int submitNewVersionDiary(DiaryDto dto, Integer userId);
}
