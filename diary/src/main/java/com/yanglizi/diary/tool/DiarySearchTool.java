package com.yanglizi.diary.tool;

import com.yanglizi.common.utils.UserContext;
import com.yanglizi.diary.pojo.dto.DiarySearchResult;
import com.yanglizi.diary.service.impl.HybridSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DiarySearchTool {

    @Autowired
    private HybridSearchService hybridSearchService;

    /**
     * 搜索日记的工具方法
     * 模型会自己判断：如果用户情绪低落，就用 highEmotion=true 的版本
     */
    @Tool(description = "搜索用户的历史日记。如果用户情绪低落，请使用 highEmotion=true 来搜索开心的日记")
    public List<String> searchDiaries(
            @ToolParam(description = "用户心情低落的主要原因") String query,
            @ToolParam(description = "是否只搜索高情绪分数日记（建议低落时用）", required = false) Boolean highEmotion
    ) {
        Integer userId = UserContext.getCurrentUserId();
        log.info("调用searchDiaries工具 query: {}, userId: {}", query, userId);
        // 调用混合检索
        List<DiarySearchResult> results = hybridSearchService.hybridSearch(query, userId, highEmotion);

        if (results.isEmpty()) {
            return List.of("没有找到相关日记");
        }

        // 返回日记内容列表
        return results.stream()
                .map(DiarySearchResult::getContent)
                .collect(Collectors.toList());
    }

}