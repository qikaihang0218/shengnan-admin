package com.shengnan.platform.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("sp_role_permission")
public class RolePermission implements Serializable {
    private Long roleId;
    private Long permissionId;
}
