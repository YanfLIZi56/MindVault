package com.yanglizi.auth.controller;

import com.yanglizi.auth.service.LogoutService;
import com.yanglizi.auth.utils.CookieUtils;
import com.yanglizi.common.anno.CurrentUser;
import com.yanglizi.common.pojo.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/logout")
public class LogoutController {

    @Autowired
    private CookieUtils cookieUtils;
    @Autowired
    private LogoutService logoutService;

    @DeleteMapping
    public Result logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieUtils.extractRefreshTokenFromCookie(request);
        logoutService.logout(refreshToken, response);
        return Result.success();
    }
}
