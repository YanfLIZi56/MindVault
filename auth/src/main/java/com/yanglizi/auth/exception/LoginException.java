package com.yanglizi.auth.exception;

import com.yanglizi.common.exception.BizException;

import static com.yanglizi.common.constant.ResponseStatus.UNAUTHORIZED_CODE;

public class LoginException extends BizException {
    public LoginException() {
        super(UNAUTHORIZED_CODE, "登录已过期, 请重新登录");
    }
}
