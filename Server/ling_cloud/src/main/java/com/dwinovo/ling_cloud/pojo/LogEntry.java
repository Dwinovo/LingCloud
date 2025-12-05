package com.dwinovo.ling_cloud.pojo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {
    private Long id;
    private String userId;
    private String username;
    private String action;
    private String status;
    private String ipAddress;
    private LocalDateTime createdAt;
}
