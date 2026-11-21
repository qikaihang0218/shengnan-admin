package com.shengnan.platform.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shengnan.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sp_permission")
public class Permission extends BaseEntity {
    private Long parentId;
    private String resourceName;
    private String resourceType;
    private String routePath;
    private String component;
    private String iconName;
    private String permission;
    private Integer sortNo;
    private Boolean isVisible;
    private Boolean isEnabled;
}
