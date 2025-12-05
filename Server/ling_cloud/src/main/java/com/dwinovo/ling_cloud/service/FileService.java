package com.dwinovo.ling_cloud.service;

import com.dwinovo.ling_cloud.dto.file.InitResponse;
import com.dwinovo.ling_cloud.pojo.File;


public interface FileService {
    
    /*
    根据H查找File表中的文件
    */
   File findByH(byte[] H);

   InitResponse init(String userId, String hashHex);

}
