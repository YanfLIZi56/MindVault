package com.yanglizi.aigc.Service;

import com.yanglizi.aigc.enums.Intent;

public interface IntentClassifier {
    Intent classify(String sessionId, String question, Integer userId);
}
