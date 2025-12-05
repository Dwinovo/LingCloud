package com.dwinovo.ling_cloud.service;

public interface LogService {
    void record(String userId, String username, String action, String status, String ipAddress);

    java.util.List<com.dwinovo.ling_cloud.pojo.LogEntry> listRecent(int limit);
}
