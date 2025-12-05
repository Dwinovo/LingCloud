package com.dwinovo.ling_cloud.dto.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InitRequest {
    private String h;     // 文件hash（Base64编码的 SHA-256）
}
