package com.yanglizi.config;

import com.yanglizi.common.serializer.Jackson2RedisSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        // key、hashKey 使用字符串序列化
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // ✅使用自己实现的通用json序列化器，替代已经移除的GenericJackson2JsonRedisSerializer
        Jackson2RedisSerializer jackson2RedisSerializer = new Jackson2RedisSerializer();
        template.setValueSerializer(jackson2RedisSerializer);
        template.setHashValueSerializer(jackson2RedisSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
