package com.yanglizi.level.exception;

import com.yanglizi.common.exception.BizException;

import static com.yanglizi.common.constant.ResponseStatus.FAIL_CODE;

public class LevelException extends BizException {
    public LevelException(String message) {
        super(FAIL_CODE, "用户等级错误: "+message);
    }
}
