package com.yanglizi.user.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RgisterUserDto {
    private String phone;
    private String nickname;
    private String password;
    private Integer gender;
    private String avatarUrl;
    private String captcha;
}
