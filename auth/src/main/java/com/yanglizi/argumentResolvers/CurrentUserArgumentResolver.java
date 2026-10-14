package com.yanglizi.argumentResolvers;

import com.yanglizi.auth.exception.UserNotFoundException;
import com.yanglizi.common.anno.CurrentUser;
import com.yanglizi.common.utils.UserContext;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @author ylz
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * 判断是否支持该参数：只有带有 @CurrentUser 注解且类型为 Integer 的参数才处理
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(Integer.class);
    }


    /**
     * 从 ThreadLocal 中获取当前用户 ID 并返回
     */
    @Override
    public @Nullable Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer, NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
        CurrentUser annotation = parameter.getParameterAnnotation(CurrentUser.class);
        boolean required = annotation == null || annotation.required();

        Integer userId = UserContext.getCurrentUserId();
        if (required && (userId == null)) {
            throw new UserNotFoundException("请先登录");
        }
        return userId;
    }
}
