package com.yanglizi.auth.controller;

import com.yanglizi.user.pojo.vo.UserLoginVo;
import com.yanglizi.auth.service.RefreshTokenService;
import com.yanglizi.auth.utils.CookieUtils;
import com.yanglizi.common.pojo.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping("/token")
public class TokenController {

    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private CookieUtils cookieUtils;

    /**
     * 刷新 Access Token
     * 方案一：从 HttpOnly Cookie 中读取 Refresh Token（Web 端推荐）
     */
    @PostMapping("/refresh")
    public Result refreshFromCookie(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieUtils.extractRefreshTokenFromCookie(request);
        UserLoginVo resp = refreshTokenService.refreshAccessToken(refreshToken, response);
        return Result.success(resp);
    }

    /**
     * 撤销 Refresh Token（登出时调用）
     */
    @PostMapping("/revoke")
    public Result revoke(HttpServletRequest request) {
        String refreshToken = cookieUtils.extractRefreshTokenFromCookie(request);
        refreshTokenService.revokeToken(refreshToken);
        return Result.success();
    }


}