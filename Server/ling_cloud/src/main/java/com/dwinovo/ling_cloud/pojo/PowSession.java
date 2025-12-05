package com.dwinovo.ling_cloud.pojo;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PowSession implements Serializable {

    private String userId;
    private String hashBase64;
    private PowStatus status;
}
