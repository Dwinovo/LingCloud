package com.dwinovo.ling_cloud.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PowStatus {
    POW  (0,"需要进行POW校验"),
    FIRST(1,"首次上传，需要POW校验"),
    ALLOW(2,"允许秒传");
     

    private final int code;
    private final String message;
}