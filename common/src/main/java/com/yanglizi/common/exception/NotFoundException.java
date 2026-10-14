package com.yanglizi.common.exception;

import static com.yanglizi.common.constant.ResponseStatus.FAIL_CODE;

public class NotFoundException extends BizException {

    public NotFoundException(Integer code,String message) {
        super(code, message + "不存在");
    }
}
