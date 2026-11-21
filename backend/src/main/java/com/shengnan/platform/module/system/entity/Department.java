package com.shengnan.platform.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shengnan.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sp_department")
public class Department extends BaseEntity {
    private Long parentId;
    private String deptName;
    private String deptCode;
    private String leaderName;
    private String contact;
    private Integer sortNo;
    private Boolean isEnabled;
}
