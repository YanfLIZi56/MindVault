package com.yanglizi.common.pojo.vo;

import lombok.Data;

import static com.yanglizi.common.constant.ResponseStatus.*;

@Data
public class Result {
    private Integer code;
    private String message;
    private Object data;

    public static Result success(Object data){
        Result result = new Result();
        result.setCode(SUCCESS_CODE);
        result.setMessage(SUCCESS);
        result.setData(data);
        return result;
    }

    public static Result success(){
        return success(null);
    }

    public static Result fail(String message){
        return fail(FAIL_CODE, message);
    }

    public static Result fail(Integer code,String message){
        Result result = new Result();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static Result fail(Integer code,String message,Object data){
        Result result = new Result();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    public static Result fail(String message,Object data){
        return fail(FAIL_CODE, message, data);
    }

    public static Result fail(Object data){
        return fail(FAIL_CODE, FAIL, data);
    }

    public static Result error(Integer code, String message) {
        Result result = new Result();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static Result error(String message) {
        return error(FAIL_CODE, message);
    }
}
