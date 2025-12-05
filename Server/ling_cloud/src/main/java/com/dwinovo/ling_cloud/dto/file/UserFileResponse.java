package com.dwinovo.ling_cloud.dto.file;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFileResponse {
    private String id;
    private String fileHash;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
