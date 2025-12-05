package com.dwinovo.ling_cloud.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PowStatus {
    POW  (0,"需要进行POW校验"),
    ALLOW(1,"允许秒传");
     

    private final int code;
    private final String message;
}