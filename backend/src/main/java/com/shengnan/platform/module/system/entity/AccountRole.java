package com.shengnan.platform.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("sp_account_role")
public class AccountRole implements Serializable {
    private Long accountId;
    private Long roleId;
}
