package com.yanglizi.diary.service;


import com.yanglizi.diary.pojo.entry.Diary;

import java.util.List;

public interface AsyncVectorService {
    void addDiaryVector(Integer diaryId);

    void addDiaryVectors(List<Diary> vectors);

    void embedDiary();
}
