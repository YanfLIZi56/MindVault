package com.yanglizi.exception.handler;

import com.yanglizi.common.exception.BizException;
import com.yanglizi.common.pojo.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result handleBizException(BizException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    // 唯一索引冲突捕获
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.error("数据库唯一性约束异常", e);
        Throwable rootCause = e.getRootCause();
        if (rootCause instanceof SQLIntegrityConstraintViolationException sqle) {
            String msg = sqle.getMessage();
            if (msg.contains("uk_phone")) {
                return Result.error("该手机号已注册！");
            }
            if (msg.contains("uk_account")) {
                return Result.error("账号已存在！");
            }
        }
        return Result.error("数据冲突，请勿重复提交");
    }

    // 兜底未知异常
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("服务器繁忙，请稍后重试");
    }
}
