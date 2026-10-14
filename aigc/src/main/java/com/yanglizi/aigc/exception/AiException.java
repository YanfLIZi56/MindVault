package com.yanglizi.aigc.exception;

import com.yanglizi.common.exception.BizException;

import static com.yanglizi.common.constant.ResponseStatus.FAIL_CODE;

public class AiException extends BizException {
    public AiException(String message) {
        super(FAIL_CODE, message);
    }
}
