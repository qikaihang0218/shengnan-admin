package com.shengnan.platform.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shengnan.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sp_dict_type")
public class DictType extends BaseEntity {
    private String dictCode;
    private String dictName;
    private Boolean isEnabled;
    private String description;
}
