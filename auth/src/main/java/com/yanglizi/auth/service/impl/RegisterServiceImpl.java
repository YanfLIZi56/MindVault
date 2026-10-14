package com.yanglizi.auth.service.impl;

import com.yanglizi.auth.async.SendAsync;
import com.yanglizi.auth.constant.CaptchaConstant;
import com.yanglizi.auth.exception.CaptchaException;
import com.yanglizi.auth.exception.NullVauleException;
import com.yanglizi.auth.exception.RegisterException;
import com.yanglizi.user.Service.UserService;
import com.yanglizi.user.pojo.dto.RgisterUserDto;
import com.yanglizi.user.pojo.entity.User;
import com.yanglizi.user.pojo.vo.UserLoginVo;
import com.yanglizi.auth.service.RegisterService;
import com.yanglizi.auth.utils.CookieUtils;
import com.yanglizi.auth.utils.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static com.yanglizi.auth.constant.TokenConstant.REFRESH_TOKEN_KEY;
import static com.yanglizi.auth.constant.TokenConstant.USER_ID_TOKEN_KEY;

@Service
@Slf4j
public class RegisterServiceImpl implements RegisterService {

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private UserService userService;
    @Autowired
    private SendAsync sendAsync;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private CookieUtils cookieUtils;

    @Value("${jwt.refreshToken.expireTime}")
    private int refreshTokenExpireTime;

    @Override
    public UserLoginVo register(RgisterUserDto user, HttpServletResponse response) {

        if (user.getPhone() == null || user.getPassword() == null){
            throw new NullVauleException("手机号和密码不能为空");
        }

        String captcha = (String) redisTemplate.opsForValue().get(CaptchaConstant.PHONE_CAPTCHA_KEY + user.getPhone());
        redisTemplate.delete(CaptchaConstant.PHONE_CAPTCHA_KEY + user.getPhone());
        if (captcha == null || !captcha.equals(user.getCaptcha())){
            throw new CaptchaException();
        }

        // 注册
        User targetUser = userService.register(user);

        UserLoginVo userLoginVo;
        if (targetUser != null){
            // 用户登录视图
            userLoginVo = genLonginUser(targetUser);
            // 生成刷新令牌
            String newRefreshToken = UUID.randomUUID().toString();
            cookieUtils.setRefreshTokenCookie(response, newRefreshToken);
            // 存储刷新令牌
            redisTemplate.opsForValue().set(REFRESH_TOKEN_KEY+newRefreshToken, targetUser.getId(), Duration.ofSeconds(refreshTokenExpireTime));
            redisTemplate.opsForValue().set(USER_ID_TOKEN_KEY+targetUser.getId(), newRefreshToken, Duration.ofSeconds(refreshTokenExpireTime));
        }else {
            throw new RegisterException();
        }

        // 发送短信
        sendAsync.sendPhone(user.getPhone(),"注册成功, 账号: " + targetUser.getAccount());

        return userLoginVo;
    }

    private UserLoginVo genLonginUser(User targetUser) {
        UserLoginVo userLoginVo = new UserLoginVo();
        userLoginVo.setId(targetUser.getId());
        userLoginVo.setAccount(targetUser.getAccount());
        userLoginVo.setNickname(targetUser.getNickname());
        userLoginVo.setAvatarUrl(targetUser.getAvatarUrl());
        userLoginVo.setLevel(targetUser.getLevel());
        userLoginVo.setAccessToken(jwtUtils.createAccessToken(Map.of("userId", targetUser.getId())));
        return userLoginVo;
    }
}
