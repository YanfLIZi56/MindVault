package com.yanglizi.auth.exception;

import com.yanglizi.common.exception.NotFoundException;

import static com.yanglizi.common.constant.ResponseStatus.UNAUTHORIZED_CODE;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(String message) {
        super(UNAUTHORIZED_CODE, message);
    }
}
