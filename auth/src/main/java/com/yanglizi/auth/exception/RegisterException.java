package com.yanglizi.auth.exception;

import com.yanglizi.common.exception.BizException;

import static com.yanglizi.common.constant.ResponseStatus.FAIL_CODE;

public class RegisterException extends BizException {
    public RegisterException() {
        super(FAIL_CODE, "注册失败, 请重试");
    }

    public RegisterException(String message) {
        super(FAIL_CODE, message);
    }
}