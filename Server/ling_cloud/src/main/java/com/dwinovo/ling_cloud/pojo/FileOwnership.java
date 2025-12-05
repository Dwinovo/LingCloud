package com.dwinovo.ling_cloud.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileOwnership {

    private String id;
    private String fileId;
    private String filesHash;
    private String userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
