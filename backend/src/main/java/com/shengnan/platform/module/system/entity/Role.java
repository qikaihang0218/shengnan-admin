package com.shengnan.platform.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shengnan.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sp_role")
public class Role extends BaseEntity {
    private String roleCode;
    private String roleName;
    private Integer sortNo;
    private String dataScope;
    private Boolean isEnabled;
    private String description;
}
