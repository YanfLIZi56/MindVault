package com.yanglizi.user.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginUserDto {

    // 账号登录
    private String account;
    private String password;

    // 手机号登录
    private String phone;
    private String captcha;

    // 都要图片验证码
    private String imgCaptcha;
    private String imgCaptchaKey;
}
