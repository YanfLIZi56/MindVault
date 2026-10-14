package com.yanglizi.common.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.StandardCharsets;

/**
 * Spring‑data‑redis4.x 替代被移除的 GenericJackson2JsonRedisSerializer
 * Jackson2(com.fasterxml)，支持LocalDateTime，写入@class类型信息，不需要Serializable
 */
public class Jackson2RedisSerializer implements RedisSerializer<Object> {

    private static final byte[] EMPTY = new byte[0];
    private final ObjectMapper objectMapper;

    public Jackson2RedisSerializer() {
        objectMapper = new ObjectMapper();
        // 支持Java8时间类型 LocalDateTime/LocalDate
        objectMapper.registerModule(new JavaTimeModule());
        // 写入类型信息，反序列化自动还原POJO
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );
    }

    @Override
    public byte[] serialize(Object value) throws SerializationException {
        if (value == null) {
            return EMPTY;
        }
        try {
            return objectMapper.writeValueAsString(value).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new SerializationException("序列化失败", e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            String json = new String(bytes, StandardCharsets.UTF_8);
            JavaType javaType = TypeFactory.defaultInstance().constructType(Object.class);
            return objectMapper.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            throw new SerializationException("反序列化失败", e);
        }
    }
}
