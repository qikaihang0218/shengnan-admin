package com.shengnan.platform.module.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shengnan.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sp_job")
public class Job extends BaseEntity {
    private String jobName;
    private String jobGroup;
    private String jobKind;
    private String invokeTarget;
    private String scriptBody;
    private String cronExpr;
    private Boolean allowConcurrent;
    private Boolean isEnabled;
    private String description;
}
