package com.yanglizi.user.pojo.vo;

import lombok.Data;

@Data
public class UserLoginVo {
    private Integer id;
    private String account;
    private String nickname;
    private Integer level;
    private String avatarUrl;
    private String accessToken;
}
