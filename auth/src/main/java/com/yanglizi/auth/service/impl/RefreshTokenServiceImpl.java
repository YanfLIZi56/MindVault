package com.yanglizi.auth.service.impl;

import com.yanglizi.auth.exception.LoginException;
import com.yanglizi.auth.exception.UserNotFoundException;
import com.yanglizi.user.mapper.UserMapper;
import com.yanglizi.user.pojo.vo.UserLoginVo;
import com.yanglizi.auth.service.RefreshTokenService;
import com.yanglizi.auth.utils.CookieUtils;
import com.yanglizi.auth.utils.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static com.yanglizi.auth.constant.TokenConstant.*;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private CookieUtils cookieUtils;
    @Autowired
    private UserMapper userMapper;

    @Value("${jwt.refreshToken.expireTime}")
    private int refreshTokenExpireTime;

    @Override
    public UserLoginVo refreshAccessToken(String refreshToken, HttpServletResponse response) {
        String script = "local val = redis.call('GET', KEYS[1]) " +
                "redis.call('DEL', KEYS[1]) " +
                "return val";

        // 执行脚本，返回获取到的 userId, 并删除旧的 refreshToken
        Integer userId = (Integer) redisTemplate.execute(
                new DefaultRedisScript<>(script, Object.class),
                Collections.singletonList(REFRESH_TOKEN_KEY+refreshToken)
        );

        if (userId == null){
            throw new LoginException();
        }
        int i = userMapper.existId(userId);
        if (i == 0){
            throw new UserNotFoundException("用户不存在");
        }
        // 生成新的 accessToken
        String accessToken = jwtUtils.createAccessToken(Map.of("userId", userId));

        // 生成新的 refreshToken(安全性)
        genNewRefreshToken(response, userId);

        UserLoginVo userLoginVo = new UserLoginVo();
        userLoginVo.setAccessToken(accessToken);
        return userLoginVo;
    }

    private void genNewRefreshToken(HttpServletResponse response, Integer userId) {
        // 存储新的 refreshToken
        String newRefreshToken = UUID.randomUUID().toString();
        cookieUtils.setRefreshTokenCookie(response, newRefreshToken);
        redisTemplate.opsForValue().set(REFRESH_TOKEN_KEY+newRefreshToken, userId, Duration.ofSeconds(refreshTokenExpireTime));
        redisTemplate.opsForValue().set(USER_ID_TOKEN_KEY+userId, newRefreshToken);
    }

    @Override
    public void revokeToken(String refreshToken) {
        redisTemplate.delete(REFRESH_TOKEN_KEY+refreshToken);
    }
}
