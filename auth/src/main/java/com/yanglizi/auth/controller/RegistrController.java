package com.yanglizi.auth.controller;

import com.yanglizi.user.pojo.dto.RgisterUserDto;
import com.yanglizi.auth.service.RegisterService;
import com.yanglizi.common.pojo.vo.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/register")
public class RegistrController {

    @Autowired
    private RegisterService registerService;

    @PostMapping
    public Result register(@RequestBody RgisterUserDto user, HttpServletResponse response){
        return Result.success(registerService.register(user, response));
    }
}
