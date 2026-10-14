package com.yanglizi.auth.service;

import com.yanglizi.user.pojo.dto.LoginUserDto;
import com.yanglizi.user.pojo.vo.UserLoginVo;
import jakarta.servlet.http.HttpServletResponse;

public interface LoginService {
    UserLoginVo phoneLogin(LoginUserDto loginUserDto, HttpServletResponse response);

    UserLoginVo accountLogin(LoginUserDto loginUserDto, HttpServletResponse response);
}
