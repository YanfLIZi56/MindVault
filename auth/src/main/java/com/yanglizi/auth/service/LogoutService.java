package com.yanglizi.auth.service;


import jakarta.servlet.http.HttpServletResponse;

public interface LogoutService {
    void logout(String refreshToken, HttpServletResponse response);
}
