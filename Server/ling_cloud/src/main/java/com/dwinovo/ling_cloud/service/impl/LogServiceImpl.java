package com.dwinovo.ling_cloud.service.impl;

import com.dwinovo.ling_cloud.mapper.LogMapper;
import com.dwinovo.ling_cloud.pojo.LogEntry;
import com.dwinovo.ling_cloud.service.LogService;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LogServiceImpl implements LogService {

    @Autowired
    private LogMapper logMapper;

    @Override
    public void record(String userId, String username, String action, String status, String ipAddress) {
        LogEntry entry = new LogEntry(null, userId, username, action, status, ipAddress, LocalDateTime.now());
        logMapper.insert(entry);
    }

    @Override
    public java.util.List<LogEntry> listRecent(int limit) {
        int size = Math.max(1, Math.min(limit, 200));
        return logMapper.listRecent(size);
    }
}
