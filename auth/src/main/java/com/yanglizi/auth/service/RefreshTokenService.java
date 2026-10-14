package com.yanglizi.auth.service;

import com.yanglizi.user.pojo.vo.UserLoginVo;
import jakarta.servlet.http.HttpServletResponse;

public interface RefreshTokenService {
    UserLoginVo refreshAccessToken(String refreshToken, HttpServletResponse response);

    void revokeToken(String refreshToken);
}
