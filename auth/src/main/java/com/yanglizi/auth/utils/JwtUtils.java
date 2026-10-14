package com.yanglizi.auth.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component  // 需要注册为bean才能使用@Value注解
@Data
public class JwtUtils {
    @Value("${jwt.accessToken.secret-key}")
    private String secretKey;
    @Value("${jwt.accessToken.expireTime:60s}")
    private Duration accessExpirationDuration;


    // 生成 JWT 字符串
    public String createAccessToken(Map<String, Object> claims) {
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> mutableClaims = new HashMap<>(claims);
        mutableClaims.put("tokenType", "access_token");

        long expireMs = accessExpirationDuration.toMillis(); // 转为毫秒
        Date exp = new Date(System.currentTimeMillis() + expireMs);

        // 使用指定的密钥签名 JWT
        // 设置 JWT 的 payload
        // 设置过期时间,60秒后过期
        // 生成 JWT 字符串
        return Jwts.builder()
                .signWith(key)
                .claims(mutableClaims)
                .expiration(exp)
                .compact();
    }

    public Claims parseJwt(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        // 使用指定的密钥验证 JWT
        // 构建 JwtParser 对象====解析器,可以多次使用
        // 解析 JWT 字符串,返回一个 SignedClaims 对象
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}