package com.yanglizi.user.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Integer id;
    private String account;
    private String phone;
    private String email;
    private String nickname;
    private String password;
    private Integer gender;
    private String avatarUrl;
    private String bio;
    private Integer level;
    private Integer status;
    private String lastLoginAt;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
}
