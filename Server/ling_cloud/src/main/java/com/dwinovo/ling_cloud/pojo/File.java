package com.dwinovo.ling_cloud.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class File {

    private String id;
    private String hashPlain;
    private Long size;
    private String ossUrl;
    private String userId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}