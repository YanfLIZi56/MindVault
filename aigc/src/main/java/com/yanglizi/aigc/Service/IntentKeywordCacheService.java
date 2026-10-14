package com.yanglizi.aigc.Service;

import java.util.List;
import java.util.Map;

public interface IntentKeywordCacheService {
    List<String> getKeywords(String start);
    void refreshAll();
    void refreshByType(String intentType);
    Map<String, List<String>> getAllKeywords();
    void clearAll();
    long getTotalKeywordCount();
}
