package com.yanglizi.level.aop;

import com.yanglizi.common.anno.RequireLevel;
import com.yanglizi.common.exception.BizException;
import com.yanglizi.common.utils.UserContext;
import com.yanglizi.level.exception.LevelException;
import com.yanglizi.user.Service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.util.StringUtils;

import static com.yanglizi.common.constant.ResponseStatus.UNAUTHORIZED_CODE;
import static com.yanglizi.level.constant.LevelConstant.PERMISSION_CONFIG_KEY;

@Aspect
@Component
@Slf4j
public class LevelAspect {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private UserService userService;

    @Around("@annotation(requireLevel)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequireLevel requireLevel) throws Throwable {
        Integer userId = UserContext.getCurrentUserId();
        if (userId == null) {
            log.warn("用户未登录，无法获取等级");
            throw new BizException(UNAUTHORIZED_CODE,"请先登录");
        }
        int userCode = userService.getUserLevelCode(userId); // 用户当前的 code（数字）

        // 1. ⭐ 获取接口的唯一编码
        String apiKey = requireLevel.apiKey();
        int requiredCode = requireLevel.value(); // 默认等级

        // 2. ⭐ 如果注解中写了 code，优先从 Redis 查动态配置
        if (StringUtils.hasText(apiKey)) {
            String dynamicKey = (String) redisTemplate.opsForHash().get(PERMISSION_CONFIG_KEY, apiKey);
            if (dynamicKey != null) {
                try {
                    requiredCode = Integer.parseInt(dynamicKey); // 覆盖默认值
                    log.debug("接口 {} 动态配置生效，要求等级：{}", apiKey, requiredCode);
                } catch (IllegalArgumentException e) {
                    log.warn("动态配置等级 Key 无效：{}，使用默认值", dynamicKey);
                }
            }
        }

        // 4. 比较
        if (userCode < requiredCode) {
            throw new LevelException(requireLevel.message());
        }
        return joinPoint.proceed();
    }
}
