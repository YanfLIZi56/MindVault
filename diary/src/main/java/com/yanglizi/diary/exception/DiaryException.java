package com.yanglizi.diary.exception;

import com.yanglizi.common.exception.BizException;

import static com.yanglizi.common.constant.ResponseStatus.FAIL_CODE;

public class DiaryException extends BizException {
    public DiaryException(String message) {
        super(FAIL_CODE,message);
    }
}
