package com.shengnan.platform.module.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 报修工单流转日志 */
@Data
@TableName("sp_repair_log")
public class RepairLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String action;
    private Integer fromStatus;
    private Integer toStatus;
    private String operator;
    private String content;
    private LocalDateTime createdAt;
}