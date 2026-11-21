package com.shengnan.platform.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shengnan.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sp_position")
public class Position extends BaseEntity {
    private String positionName;
    private String positionCode;
    private Integer sortNo;
    private Boolean isEnabled;
    private String description;
}
