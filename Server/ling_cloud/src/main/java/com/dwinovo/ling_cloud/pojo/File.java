package com.dwinovo.ling_cloud.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class File {

    // 主键：文件哈希
    private byte[] H;           // 文件内容的哈希值（SHA-256 二进制32字节）
    private Long L;             // 文件长度（字节）

    // 加密相关字段
    private byte[] IV;          // AES-GCM的IV（12字节）
    private byte[] TAG;         // AES-GCM的TAG（16字节）
    private byte[] C;           // 文件密文（AES-GCM密文）

    // PoW相关字段
    private Long offset;        // PoW片段起始偏移
    private Long size;          // PoW片段长度
    private byte[] MAC;         // PoW片段HASH（二进制32字节）

    // 元数据
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}