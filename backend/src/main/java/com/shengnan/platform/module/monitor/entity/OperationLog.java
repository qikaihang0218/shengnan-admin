package com.shengnan.platform.module.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sp_operation_log")
public class OperationLog implements Serializable {
    private Long id;
    private String moduleName;
    private String actionDesc;
    private String methodName;
    private String requestUri;
    private String paramsJson;
    private String ipAddr;
    private String operator;
    private Integer costMs;
    private Boolean isSuccess;
    private String errorMsg;
    private LocalDateTime operatedAt;
}
