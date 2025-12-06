package com.dwinovo.ling_cloud.common;

public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(StatusEnum statusEnum) {
        super(statusEnum.getMessage());
        this.code = statusEnum.getCode();
    }

    public BusinessException(StatusEnum statusEnum, String customMessage) {
        super(customMessage);
        this.code = statusEnum.getCode();
    }

    public int getCode() {
        return code;
    }
}
