package com.shengnan.platform.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shengnan.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sp_account")
public class Account extends BaseEntity {
    private String username;
    private String passwordHash;
    private String displayName;
    private String avatarUrl;
    private String email;
    private String mobile;
    private Integer gender;
    private Long departmentId;
    private Boolean isEnabled;
}
