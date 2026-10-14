package com.yanglizi.auth.service.impl;

import com.yanglizi.auth.service.LogoutService;
import com.yanglizi.auth.utils.CookieUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;

import static com.yanglizi.auth.constant.TokenConstant.REFRESH_TOKEN_KEY;
import static com.yanglizi.auth.constant.TokenConstant.USER_ID_TOKEN_KEY;


@Service
public class LogoutServiceImpl implements LogoutService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private CookieUtils cookieUtils;

    @Override
    public void logout(String refreshToken, HttpServletResponse response) {
        String tokenKey = REFRESH_TOKEN_KEY + refreshToken;
        Integer userId = (Integer) redisTemplate.opsForValue().get(tokenKey);

        String userKey = USER_ID_TOKEN_KEY + userId;

        String script = "redis.call('DEL', KEYS[1]) redis.call('DEL', KEYS[2]) return 1";
        redisTemplate.execute(
                new DefaultRedisScript<>(script, Integer.class),
                Arrays.asList(tokenKey, userKey)
        );

        cookieUtils.deleteRefreshTokenCookie(response);
    }
}
