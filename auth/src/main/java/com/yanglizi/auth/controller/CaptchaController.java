package com.yanglizi.auth.controller;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.RandomUtil;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;

import com.yanglizi.auth.async.SendAsync;
import com.yanglizi.auth.constant.CaptchaConstant;
import com.yanglizi.common.pojo.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/captcha")
public class CaptchaController {

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private SendAsync sendAsync;

    @Value("${mindVault.captcha.type}")
    private String type;
    @Value("${mindVault.captcha.timeout}")
    private Long captchaTimeout;

    @GetMapping("/photo")
    public Result photoCaptcha(){
        Map<String,Object> map = new HashMap<>();
        SpecCaptcha specCaptcha = new SpecCaptcha(132, 40, 5);
        if ("number".equals(type)) {
            specCaptcha.setCharType(Captcha.TYPE_ONLY_NUMBER);
        } else if ("char".equals(type)) {
            specCaptcha.setCharType(Captcha.TYPE_ONLY_CHAR);
        } else {
            specCaptcha.setCharType(Captcha.TYPE_DEFAULT);
        }
        String verCode = specCaptcha.text().toLowerCase();
        String key = UUID.randomUUID().toString();
        map.put("key", key);
        map.put("captcha", specCaptcha.toBase64());
        // 存入redis并设置过期时间为5分钟
        redisTemplate.opsForValue().set(CaptchaConstant.PHOTO_CAPTCHA_KEY + key, verCode, Duration.ofMinutes(captchaTimeout));
        return Result.success(map);
    }

    @GetMapping("/phone")
    public Result phoneCaptcha(String phone){

        if (Validator.isMobile(phone)){
            String verCode = RandomUtil.randomNumbers(6);
            String key = CaptchaConstant.PHONE_CAPTCHA_KEY + phone;

            // 异步发送验证码
            sendAsync.sendPhone(phone,verCode);

            // 存入redis并设置过期时间为5分钟
            redisTemplate.opsForValue().set(key, verCode, Duration.ofMinutes(captchaTimeout));
            return Result.success();
        }else{
            return Result.fail("手机号码格式错误");
        }
    }
}
