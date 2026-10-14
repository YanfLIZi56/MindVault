package com.yanglizi.auth.service.impl;

import com.yanglizi.auth.constant.CaptchaConstant;
import com.yanglizi.auth.exception.NullVauleException;
import com.yanglizi.auth.exception.UserNotFoundException;
import com.yanglizi.user.mapper.UserMapper;
import com.yanglizi.user.pojo.dto.LoginUserDto;
import com.yanglizi.user.pojo.vo.UserLoginVo;
import com.yanglizi.auth.service.LoginService;
import com.yanglizi.auth.utils.CookieUtils;
import com.yanglizi.auth.utils.JwtUtils;
import com.yanglizi.common.exception.BizException;
import com.yanglizi.common.utils.Encrypt;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.yanglizi.auth.constant.TokenConstant.REFRESH_TOKEN_KEY;
import static com.yanglizi.auth.constant.TokenConstant.USER_ID_TOKEN_KEY;
import static com.yanglizi.common.constant.ResponseStatus.FAIL_CODE;

@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private CookieUtils cookieUtils;
    @Autowired
    private UserMapper userMapper;

    @Value("${jwt.refreshToken.expireTime}")
    private int refreshTokenExpireTime;

    @Override
    public UserLoginVo phoneLogin(LoginUserDto loginUserDto, HttpServletResponse response) {
        checkPhotoCaptcha(loginUserDto);

        if (Objects.equals(loginUserDto.getPhone(), "")){
            throw new NullVauleException("请输入手机号码");
        }
        if (Objects.equals(loginUserDto.getCaptcha(), "")){
            throw new NullVauleException("请输入短信验证码");
        }
        String captcha = (String) redisTemplate.opsForValue()
                .get(CaptchaConstant.PHONE_CAPTCHA_KEY + loginUserDto.getPhone());
        redisTemplate.delete(CaptchaConstant.PHONE_CAPTCHA_KEY + loginUserDto.getPhone());
        if (!Objects.equals(captcha, loginUserDto.getCaptcha())){
            throw new BizException(FAIL_CODE,"短信验证码错误");
        }

        UserLoginVo user = userMapper.selectLoginUser(loginUserDto);
        if (user == null){
            throw new UserNotFoundException("登录失败: 手机号未注册");
        }

        return goLogin(user, response);
    }

    @Override
    public UserLoginVo accountLogin(LoginUserDto loginUserDto, HttpServletResponse response) {
        checkPhotoCaptcha(loginUserDto);

        // 密码加密
        loginUserDto.setPassword(Encrypt.sha256(loginUserDto.getPassword()));

        UserLoginVo user = userMapper.selectLoginUser(loginUserDto);
        if (user == null){
            throw new UserNotFoundException("登录失败: 账号或密码错误");
        }

        return goLogin(user, response);
    }

    private UserLoginVo goLogin(UserLoginVo user, HttpServletResponse response) {

        user.setAccessToken(jwtUtils.createAccessToken(Map.of("userId", user.getId())));

        userMapper.updateLoginTime(user.getId(), LocalDateTime.now().toString());

        // 生成刷新令牌
        String newRefreshToken = UUID.randomUUID().toString();
        cookieUtils.setRefreshTokenCookie(response, newRefreshToken);
        // 存储刷新令牌
        redisTemplate.opsForValue().set(REFRESH_TOKEN_KEY+newRefreshToken, user.getId(), Duration.ofSeconds(refreshTokenExpireTime));
        redisTemplate.opsForValue().set(USER_ID_TOKEN_KEY+user.getId(), newRefreshToken, Duration.ofSeconds(refreshTokenExpireTime));

        return user;
    }

    private void checkPhotoCaptcha(LoginUserDto loginUserDto) {
        if (Objects.equals(loginUserDto.getImgCaptcha(), "")){
            throw new NullVauleException("请输入图片验证码");
        }
        String photoCaptcha = (String) redisTemplate.opsForValue()
                .get(CaptchaConstant.PHOTO_CAPTCHA_KEY + loginUserDto.getImgCaptchaKey());
        if (photoCaptcha == null){
            throw new NullVauleException("图片验证码已过期");
        }
        redisTemplate.delete(CaptchaConstant.PHOTO_CAPTCHA_KEY + loginUserDto.getImgCaptchaKey());
        if (!Objects.equals(photoCaptcha, loginUserDto.getImgCaptcha())){
            throw new BizException(FAIL_CODE,"图片验证码错误");
        }
    }
}
