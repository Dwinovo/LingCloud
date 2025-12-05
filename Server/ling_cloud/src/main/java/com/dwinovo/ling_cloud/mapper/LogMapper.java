package com.dwinovo.ling_cloud.mapper;

import com.dwinovo.ling_cloud.pojo.LogEntry;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LogMapper {
    int insert(LogEntry entry);

    List<LogEntry> listRecent(@Param("limit") int limit);
}
