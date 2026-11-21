package com.shengnan.platform.module.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sp_job_log")
public class JobLog implements Serializable {
    private Long id;
    private Long jobId;
    private String jobName;
    private String jobKind;
    private String targetBrief;
    private Boolean isSuccess;
    private String logMessage;
    private Integer costMs;
    private LocalDateTime firedAt;
}
