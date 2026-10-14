package com.yanglizi.auth.controller;

import com.yanglizi.user.pojo.dto.LoginUserDto;
import com.yanglizi.auth.service.LoginService;
import com.yanglizi.common.pojo.vo.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @PostMapping("/phone")
    public Result phoneLogin(@RequestBody LoginUserDto loginUserDto, HttpServletResponse response) {
        return Result.success(loginService.phoneLogin(loginUserDto, response));
    }

    @PostMapping("/account")
    public Result accountLogin(@RequestBody LoginUserDto loginUserDto, HttpServletResponse response) {
        return Result.success(loginService.accountLogin(loginUserDto, response));
    }
}
