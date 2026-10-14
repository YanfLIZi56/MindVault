package com.yanglizi.interceptor;

import com.yanglizi.auth.exception.UserNotFoundException;
import com.yanglizi.auth.utils.JwtUtils;
import com.yanglizi.common.utils.UserContext;
import com.yanglizi.exception.TokenException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.yanglizi.auth.constant.TokenConstant.*;

@Component
@Slf4j
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从 Header 获取 Token
        String token = request.getHeader(AUTHORIZATION);

        // 2. 如果没有 Token，放行（让匿名用户也能访问公开接口）
        if (token == null || !token.startsWith(PREFIX)) {
            log.debug("未携带 Token，视为匿名用户");
            return true;
        }

        // 3. 提取 Token 并校验
        String jwt = token.substring(PREFIX_LENGTH);
        try {
            Claims claims = jwtUtils.parseJwt(jwt);
            if (claims == null) {
                log.warn("解析 JWT 返回 null，Token: {}", jwt);
                throw new UserNotFoundException("Token 无效");
            }

        // 4. 解析 userId 并存入 ThreadLocal
            Integer userId = claims.get("userId", Integer.class);
            if (userId == null){
                throw new UserNotFoundException("Token 格式错误");
            }

            UserContext.setCurrentUserId(userId);
            log.debug("用户认证成功，userId: {}", userId);
        } catch (Exception e) {
            log.warn("Token 无效或已过期: {}, 异常: {}", jwt, e.getMessage());
            log.debug("需要刷新 token");
            throw new TokenException("refresh_token");
        }

        return true;
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        UserContext.clear();
    }
}
