package com.yanglizi.auth.exception;

import com.yanglizi.common.exception.BizException;

import static com.yanglizi.common.constant.ResponseStatus.FAIL_CODE;

public class NullVauleException extends BizException {
    public NullVauleException(String message) {
        super(FAIL_CODE, message);
    }
}
