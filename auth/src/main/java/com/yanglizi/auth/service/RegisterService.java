package com.yanglizi.auth.service;

import com.yanglizi.user.pojo.dto.RgisterUserDto;
import com.yanglizi.user.pojo.vo.UserLoginVo;
import jakarta.servlet.http.HttpServletResponse;

public interface RegisterService {
    UserLoginVo register(RgisterUserDto user, HttpServletResponse response);
}
