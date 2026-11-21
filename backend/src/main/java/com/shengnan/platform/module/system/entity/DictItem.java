package com.shengnan.platform.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shengnan.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sp_dict_item")
public class DictItem extends BaseEntity {
    private Long typeId;
    private String itemLabel;
    private String itemValue;
    private Integer sortNo;
    private Boolean isEnabled;
}
