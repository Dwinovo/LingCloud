package com.dwinovo.ling_cloud.utils;

import com.aliyun.oss.OSS;
import com.dwinovo.ling_cloud.common.BusinessException;
import com.dwinovo.ling_cloud.pojo.AliyunOSSProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Component
public class OSSUtils {

    @Autowired
    private OSS ossClient; // 通过配置类注入OSS客户端

    @Autowired
    private AliyunOSSProperties properties; // 注入我们自定义的配置

    
    private String uploadBytes(byte[] data, String extension, String namespace) {
        String bucketName = properties.getBucketName();
        String endpoint = properties.getEndpoint();
        String objectName = namespace + "/" + UUID.randomUUID().toString() + extension;
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            ossClient.putObject(bucketName, objectName, inputStream);
        } catch (Exception e) {
            log.error("字节数据上传到OSS失败", e);
            throw new RuntimeException("文件上传失败，请稍后重试", e);
        }
        return "https://" + bucketName + "." + endpoint + "/" + objectName;
    }


    /**
     * 从OSS删除文件
     *
     * @param url 待删除文件的完整URL
     */
    public void delete(String url) {
        // 1. 获取配置信息
        String bucketName = properties.getBucketName();
        String endpoint = properties.getEndpoint();

        // 2. 从URL中解析出Object Name
        //    URL格式: https://bucket-name.endpoint/object-name
        String prefix = "https://" + bucketName + "." + endpoint + "/";
        if (!url.startsWith(prefix)) {
            log.error("无效的OSS URL: {}", url);
            throw new BusinessException("提供的阿里云URL不是一个有效的OSS文件地址");
        }
        String objectName = url.substring(prefix.length());

        // 3. 执行删除
        try {
            ossClient.deleteObject(bucketName, objectName);
        } catch (Exception e) {
            log.error("从OSS删除文件失败, objectName: {}", objectName, e);
            throw new BusinessException("阿里云文件删除失败，请稍后重试");
        }
    }
}
