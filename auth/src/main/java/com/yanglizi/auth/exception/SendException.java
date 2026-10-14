package com.yanglizi.auth.exception;

import com.yanglizi.common.exception.BizException;

import static com.yanglizi.common.constant.ResponseStatus.FAIL_CODE;

public class SendException extends BizException {
    public SendException() {
        super(FAIL_CODE, "发送信息失败");
    }

    public SendException(String message) {
        super(FAIL_CODE, message);
    }
}