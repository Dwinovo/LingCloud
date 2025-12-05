package com.dwinovo.ling_cloud.service;

import com.dwinovo.ling_cloud.dto.file.FileDownloadResponse;
import com.dwinovo.ling_cloud.dto.file.InitResponse;
import com.dwinovo.ling_cloud.pojo.File;
import java.util.List;


public interface FileService {
    
    /*
    根据H查找File表中的文件
    */
   File findByH(String hashBase64);

   File findById(String id);

   InitResponse init(String userId, String hashBase64);

   void processPowUpload(byte[] payload, String userId, String originalFileName);

   List<File> listUserFiles(String userId);

   FileDownloadResponse getFileDownloadInfo(String userId, String fileId);

   void deleteFile(String userId, String fileId);

}
