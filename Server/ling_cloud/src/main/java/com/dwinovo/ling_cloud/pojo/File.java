package com.dwinovo.ling_cloud.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class File {

    private String id;          // 文件主键ID（UUID）
    private String fileHash;    // base64编码的文件哈希

    // 加密相关字段
    private String iv;          // base64编码的IV
    private String tag;         // base64编码的TAG
    private String fileUrl;     // 存储密文的URL
    private String mac;         // base64编码的MAC
    private String name;        // 原始文件名

    // 元数据
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
