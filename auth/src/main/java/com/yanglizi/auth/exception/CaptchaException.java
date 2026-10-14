package com.yanglizi.auth.exception;

import com.yanglizi.common.exception.BizException;

import static com.yanglizi.common.constant.ResponseStatus.FAIL_CODE;

public class CaptchaException extends BizException {
    public CaptchaException() {
        super(FAIL_CODE, "验证码错误或已过期");
    }
    
    public CaptchaException(String message) {
        super(FAIL_CODE, message);
    }
}