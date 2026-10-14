package com.yanglizi.user.Service.impl;

import cn.hutool.core.util.RandomUtil;
import com.yanglizi.common.utils.Encrypt;
import com.yanglizi.level.Service.LevelService;
import com.yanglizi.level.exception.LevelException;
import com.yanglizi.level.pojo.dto.LevelCacheDto;
import com.yanglizi.user.Service.UserService;
import com.yanglizi.user.mapper.UserMapper;
import com.yanglizi.user.pojo.dto.RgisterUserDto;
import com.yanglizi.user.pojo.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private LevelService levelService;

    @Value("${mindVault.account-charset}")
    private String accountStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abccdefghijklmnopqrstuvwxyz";
    @Value("${mindVault.account-length}")
    private int accountLength = 8;

    @Override
    public User register(RgisterUserDto user) {
        User targetUser = new User();
        /* 生成账号 */
        targetUser.setAccount(genAccount());

        targetUser.setPhone(user.getPhone());
        targetUser.setNickname(user.getNickname());
        targetUser.setGender(user.getGender());
        targetUser.setAvatarUrl(user.getAvatarUrl());
        targetUser.setLastLoginAt(LocalDateTime.now().toString());

        /* 密码加密 */
        targetUser.setPassword(Encrypt.sha256(user.getPassword()));

        /* 刚注册, 最低等级用户 */
        LevelCacheDto defaultLevel = levelService.getLevelByRule(0);
        if (defaultLevel != null){
            targetUser.setLevel(defaultLevel.getId());
        }

        int i = userMapper.createUser(targetUser);
        if (i > 0){
            log.debug("用户注册成功：{}", targetUser);
            return targetUser;
        }
        return null;
    }

    @Override
    public Integer getUserLevelCode(Integer userId) {
        Integer levelId = userMapper.getUserLevelId(userId);
        Integer levelCode = levelService.getLevelByCode(levelId);;
        if (levelCode == null || levelCode <= 0){
            throw new LevelException("用户等级被删除, 请联系管理员");
        }
        return levelCode;
    }

    private String genAccount() {
        int i = 10;
        while (i > 0){
            String account = RandomUtil.randomString(accountStr,accountLength);
            if (userMapper.existAccount(account) == 0){
                log.debug("生成账号成功：{}", account);
                return account;
            }
            i--;
        }
        String fallback = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        log.warn("随机账号生成失败（10次碰撞），降级为时间戳账号：{}", fallback);
        return fallback;
    }
}
