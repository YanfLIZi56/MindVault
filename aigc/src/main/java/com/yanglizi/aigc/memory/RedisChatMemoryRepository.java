package com.yanglizi.aigc.memory;

import cn.hutool.json.JSONUtil;
import lombok.Data;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.util.ArrayList;
import java.util.List;

import static com.yanglizi.aigc.constants.ChatConstant.MEMORY_KEY;

@Data
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private final String memoryKey;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public RedisChatMemoryRepository(String memoryKey) {
        this.memoryKey = memoryKey;
    }

    public RedisChatMemoryRepository(){
        this(MEMORY_KEY);
    }

    @Override
    public List<String> findConversationIds() {
        List<String> sessionIdList = new ArrayList<>();
        Cursor<String> scan = redisTemplate.scan(
                ScanOptions
                        .scanOptions()
                        .match(memoryKey + "*")
                        .count(200)
                        .build()
        );
        scan.forEachRemaining(key -> {
            String sessionId = key.replace(memoryKey, "");
            sessionIdList.add(sessionId);
        });
        return sessionIdList;
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = memoryKey + conversationId;
        BoundListOperations<String, String> ops = redisTemplate.boundListOps(key);
        return ops.range(0, -1).stream().map(MessageUtil::toMessage).toList();
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String key = memoryKey + conversationId;
        BoundListOperations<String, String> ops = redisTemplate.boundListOps(key);

        deleteByConversationId(conversationId);

        messages.forEach(msg -> ops.rightPush(MessageUtil.toJson(msg)));
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        redisTemplate.delete(memoryKey + conversationId);

    }
}
