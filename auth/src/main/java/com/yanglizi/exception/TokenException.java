package com.yanglizi.exception;

import com.yanglizi.common.exception.BizException;

import java.net.BindException;

import static com.yanglizi.common.constant.ResponseStatus.REFRESH_TOKEN_CODE;

public class TokenException extends BizException {

    public TokenException(String message) {
        super(REFRESH_TOKEN_CODE, message);
    }
}
