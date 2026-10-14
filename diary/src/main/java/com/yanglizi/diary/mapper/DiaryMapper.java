package com.yanglizi.diary.mapper;

import com.yanglizi.diary.pojo.dto.DiarySearchResult;
import com.yanglizi.diary.pojo.dto.QueryDiaryDto;
import com.yanglizi.diary.pojo.entry.Diary;
import com.yanglizi.diary.pojo.vo.DiaryVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DiaryMapper {

    int insert(Diary diary);

    void updateStatusAndContent(Integer diaryId, Integer status, String content);

    int update(Diary diary);

    Diary selectDiaryById(Integer id);

    void closeCurrent(String versionGroup, Integer userId);

    List<Integer> selectCurrentDiaryIdsByEmotionScore(Integer userId, Integer emotionScore);

    /**
     * 全文检索：使用 GIN 倒排索引 + ts_rank 排序（近似 BM25）
     */
    List<DiarySearchResult> searchByFullText(@Param("query") String query,
                                             @Param("userId") Integer userId,
                                             @Param("limit") Integer limit);

    List<Diary> selectByIds(List<Integer> ids);

    DiaryVo getDiaryById(Integer id, Integer userId);

    List<Diary> getDiaryListByVersionGroup(Integer userId, String versionGroup);

    List<DiaryVo> getDiaryList(QueryDiaryDto dto);

    List<DiaryVo> getDiaryListByStatus(QueryDiaryDto dto);

    int getStatusById(Integer id, String versionGroup, Integer userId);
}

