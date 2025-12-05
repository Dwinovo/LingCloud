package com.dwinovo.ling_cloud.dto.file;

import com.dwinovo.ling_cloud.pojo.PowStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InitResponse {
    private int status;
    private String message;
    private String iv;

    public InitResponse(PowStatus status) {
        this(status, null);
    }

    public InitResponse(PowStatus status, String iv) {
        this.status = status.getCode();
        this.message = status.getMessage();
        this.iv = iv;
    }
}
