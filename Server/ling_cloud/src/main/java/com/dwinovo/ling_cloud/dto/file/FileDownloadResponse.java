package com.dwinovo.ling_cloud.dto.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileDownloadResponse {
    private String iv;
    private String tag;
    private String url;
}
