package com.yanglizi.auth.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.yanglizi.auth.constant.TokenConstant.REFRESH_TOKEN;


@Component
public class CookieUtils {
    @Value("${jwt.refreshToken.expireTime}")
    private int refreshTokenExpireTime;

    /**
     * 设置 Refresh Token Cookie
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN, refreshToken);

        // ⚠️ 核心：JS 无法读取，防 XSS 窃取
        cookie.setHttpOnly(true);

        // ⚠️ 核心：必须 HTTPS（本地 localhost 开发可先设为 false，上线必须 true）
        // 原理是 Cookie 默认是 HTTP Only，但 JS 仍然可以访问，设置 Secure 后，JS 无法访问，但 HTTP 请求中可以携带
        cookie.setSecure(true);

        // 路径设为根路径，确保所有接口请求都带上
        cookie.setPath("/");

        cookie.setMaxAge(refreshTokenExpireTime);

        response.addCookie(cookie);
    }

    /**
     * 从 Cookie 中提取 Refresh Token
     */
    public String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (REFRESH_TOKEN.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 删除 Refresh Token Cookie
     */
    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

    }
}