package com.dwinovo.ling_cloud.common;

public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(4001, message);  // 给个默认业务错误码
    }

    public int getCode() {
        return code;
    }
}
