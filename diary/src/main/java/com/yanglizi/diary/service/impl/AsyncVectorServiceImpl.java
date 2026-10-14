package com.yanglizi.diary.service.impl;

import com.yanglizi.diary.mapper.DiaryMapper;
import com.yanglizi.diary.pojo.entry.Diary;
import com.yanglizi.diary.service.AsyncVectorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AsyncVectorServiceImpl implements AsyncVectorService {
    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private DiaryMapper diaryMapper;

    // 标记为已废弃
    @Deprecated
    @Override
    public void embedDiary() {
        Document document = new Document("小猫真可爱");
        document.getMetadata().put("id", "123");

        float[] embed = embeddingModel.embed(document);

        System.out.println(Arrays.toString(embed)+"\n 维度: "+embed.length);
    }

    @Async
    @Override
    public void addDiaryVector(Integer diaryId) {
        Diary diary = diaryMapper.selectDiaryById(diaryId);
        Document doc = new Document(
                diary.getContent(),
                Map.of(
                        "diary_id", diaryId,
                        "user_id", diary.getUserId()
                )
        );
        vectorStore.add(List.of(doc));
        log.debug("向量已添加: diaryId={}, userId={}", diaryId, diary.getUserId());
    }

    @Override
    public void addDiaryVectors(List<Diary> vectors) {
        List<Document> documents = vectors.stream()
                .map(v -> new Document(
                        v.getContent(),
                        Map.of(
                                "diary_id", v.getId(),
                                "user_id", v.getUserId()
                        )
                ))
                .collect(Collectors.toList());
        vectorStore.add(documents);
        log.debug("批量向量添加完成: count={}", documents.size());
    }
}
