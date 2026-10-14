package com.yanglizi.common.exception;

import lombok.Getter;

import static com.yanglizi.common.constant.ResponseStatus.FAIL_CODE;

@Getter
public class BizException extends RuntimeException {
    private final Integer code;
    
    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}