package com.dwinovo.ling_cloud.controller;

import com.dwinovo.ling_cloud.common.ApiResponse;
import com.dwinovo.ling_cloud.dto.log.OperationLogResponse;
import com.dwinovo.ling_cloud.pojo.LogEntry;
import com.dwinovo.ling_cloud.service.LogService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    @Autowired
    private LogService logService;

    @GetMapping
    public ApiResponse<List<OperationLogResponse>> listLogs(@RequestParam(value = "limit", defaultValue = "50") int limit) {
        List<LogEntry> entries = logService.listRecent(limit);
        List<OperationLogResponse> response = entries.stream()
            .map(entry -> new OperationLogResponse(
                entry.getId(),
                entry.getUserId(),
                entry.getUsername(),
                entry.getAction(),
                entry.getStatus(),
                entry.getIpAddress(),
                entry.getCreatedAt()
            ))
            .collect(Collectors.toList());
        return ApiResponse.success(response);
    }
}
