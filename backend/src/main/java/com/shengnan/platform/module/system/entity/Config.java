package com.shengnan.platform.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shengnan.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sp_config")
public class Config extends BaseEntity {
    private String configKey;
    private String configValue;
    private String configName;
    private Boolean isBuiltin;
    private String description;
}
