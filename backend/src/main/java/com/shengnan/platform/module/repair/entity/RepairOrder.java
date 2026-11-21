package com.shengnan.platform.module.repair.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shengnan.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/** 报修工单 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sp_repair_order")
public class RepairOrder extends BaseEntity {
    private String orderNo;
    private String title;
    private String category;
    private Integer priority;      // 0低 1中 2高 3紧急
    private Integer status;        // 0待受理 1已派单 2处理中 3已完成 4已关闭
    private String location;
    private String description;
    /** 故障照片：文件ID逗号分隔 */
    private String images;
    private String contactName;
    private String contactPhone;
    private Long applicantId;
    private String applicantName;
    private Long deptId;
    private Long handlerId;
    private String handlerName;
    private LocalDateTime expectTime;
    private LocalDateTime assignTime;
    private LocalDateTime finishTime;
    private String resultDesc;
    private Integer rating;
    private String remark;

    /** 非表字段：流转日志 */
    @TableField(exist = false)
    private List<RepairLog> logs;
}