package com.yanglizi.diary.service.impl;

import com.yanglizi.diary.mapper.DiaryMapper;
import com.yanglizi.diary.pojo.dto.DiarySearchResult;
import com.yanglizi.diary.pojo.entry.Diary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HybridSearchService {

    public static final int EMOTION_SCORE = 5;
    @Autowired
    private DiaryMapper diaryMapper;

    @Autowired
    private VectorStore vectorStore;

    // 每个召回源取前 N 条
    private static final int CANDIDATE_SIZE = 3;
    // 最终返回 Top K
    private static final int FINAL_TOP_K = 5;
    // RRF 融合参数 k（通常取 60）
    private static final int RRF_K = 60;

    /**
     * 混合检索主入口
     * @param query 用户输入（关键词或自然语言）
     * @param userId 用户ID
     * @param highEmotion 是否只检索高情绪分数日记（低落时使用）
     * @return 去重、融合、排序后的日记列表（最多 FINAL_TOP_K 条）
     */
    public List<DiarySearchResult> hybridSearch(String query, Integer userId, Boolean highEmotion) {
        long start = System.currentTimeMillis();

        // 1. 如果 highEmotion=true，先获取高情绪分日记 ID 列表，用于过滤
        List<Integer> emotionFilterIds = null;
        if (Boolean.TRUE.equals(highEmotion)) {
            emotionFilterIds = diaryMapper.selectCurrentDiaryIdsByEmotionScore(userId, EMOTION_SCORE);
            if (emotionFilterIds == null || emotionFilterIds.isEmpty()) {
                log.info("高情绪分日记为空，直接返回空结果");
                return Collections.emptyList();
            }
        }

        // 2. 并行执行全文检索和向量检索
        List<DiarySearchResult> fullTextResults = searchFullText(query, userId, emotionFilterIds);
        List<DiarySearchResult> vectorResults = searchVector(query, userId, emotionFilterIds);

        // 3. 日志：召回情况
        log.info("全文检索召回: {} 条", fullTextResults.size());
        log.info("向量检索召回: {} 条", vectorResults.size());

        // 4. RRF 融合 + 去重
        List<DiarySearchResult> merged = mergeAndRerank(fullTextResults, vectorResults);

        // 5. 补充日记内容（从数据库取完整 content）
        if (!merged.isEmpty()) {
            List<Integer> ids = merged.stream().map(DiarySearchResult::getId).collect(Collectors.toList());
            List<Diary> diaries = diaryMapper.selectByIds(ids);
            Map<Integer, String> contentMap = diaries.stream()
                    .collect(Collectors.toMap(Diary::getId, Diary::getContent));
            merged.forEach(r -> r.setContent(contentMap.getOrDefault(r.getId(), "内容已删除")));
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("混合检索完成，耗时 {}ms，最终返回 {} 条", elapsed, merged.size());

        // 输出召回率日志
        logRecallStats(query, fullTextResults, vectorResults, merged);

        return merged;
    }

    /**
     * 全文检索（使用 GIN 索引 + ts_rank）
     */
    private List<DiarySearchResult> searchFullText(String query, Integer userId, List<Integer> emotionFilterIds) {
        // 如果情绪过滤列表不为空，需要额外过滤
        // 但 Mapper 方法里没有加条件，我们在这里通过查询后过滤或者修改 SQL
        // 简单起见，我们直接调用并后过滤
        List<DiarySearchResult> results = diaryMapper.searchByFullText(query, userId, CANDIDATE_SIZE);
        if (results == null) {
            return Collections.emptyList();
        }

        // 如果 emotionFilterIds 不为空，过滤掉不在列表中的结果
        if (emotionFilterIds != null) {
            Set<Integer> filterSet = new HashSet<>(emotionFilterIds);
            results = results.stream()
                    .filter(r -> filterSet.contains(r.getId()))
                    .collect(Collectors.toList());
        }
        results.forEach(r -> r.setSource("fulltext"));
        return results;
    }

    /**
     * 向量检索（使用 PGVector）
     */
    private List<DiarySearchResult> searchVector(String query, Integer userId, List<Integer> emotionFilterIds) {
        // 1. 获取有效日记 ID（如果有情绪过滤，使用过滤后的 ID 作为向量检索的过滤条件）
        List<Integer> validIds = emotionFilterIds != null ?
                emotionFilterIds :
                diaryMapper.selectCurrentDiaryIdsByEmotionScore(userId, EMOTION_SCORE);
        if (validIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 构建过滤表达式：diary_id IN (id1, id2, ...)
        String filterExpr = validIds.stream()
                .map(id -> "diary_id == " + id)
                .collect(Collectors.joining(" || "));

        // 3. 执行向量检索
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(CANDIDATE_SIZE)
                .similarityThreshold(0.3)   // 阈值设低一点，让 RRF 来过滤
                .filterExpression(filterExpr)
                .build();

        List<Document> docs = vectorStore.similaritySearch(request);
        List<DiarySearchResult> results = new ArrayList<>();
        for (Document doc : docs) {
            Object diaryIdObj = doc.getMetadata().get("diary_id");
            if (diaryIdObj == null) {
                continue;
            }
            Double score = (Double) doc.getMetadata().getOrDefault("score", 0.0);
            DiarySearchResult r = DiarySearchResult.builder()
                    .id(Integer.valueOf(diaryIdObj.toString()))
                    .vectorScore(score)
                    .source("vector")
                    .build();
            results.add(r);
        }
        return results;
    }

    /**
     * RRF 融合 + 去重 + 排序
     */
    private List<DiarySearchResult> mergeAndRerank(List<DiarySearchResult> fullTextResults,
                                                    List<DiarySearchResult> vectorResults) {
        // 构建排名映射：diary_id → 排名（从 1 开始）
        Map<Integer, Integer> fullTextRank = getRankMap(fullTextResults);
        Map<Integer, Integer> vectorRank = getRankMap(vectorResults);

        Set<Integer> allIds = new HashSet<>();
        allIds.addAll(fullTextRank.keySet());
        allIds.addAll(vectorRank.keySet());

        List<DiarySearchResult> merged = new ArrayList<>();
        for (Integer id : allIds) {
            int rank1 = fullTextRank.getOrDefault(id, Integer.MAX_VALUE);
            int rank2 = vectorRank.getOrDefault(id, Integer.MAX_VALUE);

            double rrfScore = 0.0;
            if (rank1 != Integer.MAX_VALUE) {
                rrfScore += 1.0 / (RRF_K + rank1);
            }
            if (rank2 != Integer.MAX_VALUE) {
                rrfScore += 1.0 / (RRF_K + rank2);
            }

            DiarySearchResult r = DiarySearchResult.builder()
                    .id(id)
                    .finalScore(rrfScore)
                    .fullTextScore(rank1 != Integer.MAX_VALUE ? 1.0 / (RRF_K + rank1) : 0)
                    .vectorScore(rank2 != Integer.MAX_VALUE ? 1.0 / (RRF_K + rank2) : 0)
                    .source(rank1 != Integer.MAX_VALUE && rank2 != Integer.MAX_VALUE ? "both" :
                            rank1 != Integer.MAX_VALUE ? "fulltext" : "vector")
                    .build();
            merged.add(r);
        }

        // 按最终分数降序排列
        merged.sort((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()));
        return merged.stream().limit(FINAL_TOP_K).collect(Collectors.toList());
    }

    private Map<Integer, Integer> getRankMap(List<DiarySearchResult> results) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < results.size(); i++) {
            map.put(results.get(i).getId(), i + 1);
        }
        return map;
    }

    /**
     * 日志输出召回率分析
     */
    private void logRecallStats(String query,
                                 List<DiarySearchResult> fullTextResults,
                                 List<DiarySearchResult> vectorResults,
                                 List<DiarySearchResult> finalResults) {
        Set<Integer> fullTextIds = fullTextResults.stream()
                .map(DiarySearchResult::getId).collect(Collectors.toSet());
        Set<Integer> vectorIds = vectorResults.stream()
                .map(DiarySearchResult::getId).collect(Collectors.toSet());

        Set<Integer> intersection = new HashSet<>(fullTextIds);
        intersection.retainAll(vectorIds);

        Set<Integer> union = new HashSet<>(fullTextIds);
        union.addAll(vectorIds);

        Set<Integer> finalIds = finalResults.stream()
                .map(DiarySearchResult::getId).collect(Collectors.toSet());

        long recalledInFinal = finalIds.stream().filter(union::contains).count();

        log.info("=== 混合检索召回率分析 ===");
        log.info("查询词: {}", query);
        log.info("全文检索召回: {}", fullTextIds.size());
        log.info("向量检索召回: {}", vectorIds.size());
        log.info("两路交集: {}", intersection.size());
        log.info("总召回（去重后）: {}", union.size());
        log.info("最终返回 Top K: {}", finalIds.size());
        if (!finalIds.isEmpty()) {
            log.info("最终结果中有 {} 条来自召回集合，召回覆盖率: {:.2f}%",
                    recalledInFinal, (double) recalledInFinal / finalIds.size() * 100);
        }
        log.info("====================================");
    }
}