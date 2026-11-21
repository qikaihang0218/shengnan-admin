package com.shengnan.platform.module.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shengnan.platform.common.BusinessException;
import com.shengnan.platform.common.CrudService;
import com.shengnan.platform.module.system.entity.*;
import com.shengnan.platform.module.system.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleService extends CrudService<RoleMapper, Role> {
    private final RoleMapper mapper;
    private final RolePermissionMapper rolePermissionMapper;

    public RoleService(RoleMapper mapper, RolePermissionMapper rolePermissionMapper) {
        this.mapper = mapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    @Override protected RoleMapper mapper() { return mapper; }

    public List<Role> listAllEnabled() {
        return mapper.selectList(Wrappers.<Role>lambdaQuery()
                .eq(Role::getIsEnabled, true).orderByAsc(Role::getSortNo));
    }

    @Transactional
    public void createRole(Role role) {
        if (mapper.selectCount(Wrappers.<Role>lambdaQuery().eq(Role::getRoleCode, role.getRoleCode())) > 0) {
            throw new BusinessException("角色编码已存在");
        }
        role.setId(null);
        mapper.insert(role);
    }

    @Transactional
    public void updateRole(Role role) {
        Role old = mapper.selectById(role.getId());
        if (old == null) throw new BusinessException("角色不存在");
        role.setRoleCode(null);
        if ("super_admin".equals(old.getRoleCode())) {
            role.setIsEnabled(true); // 超管角色不可停用
        }
        mapper.updateById(role);
    }

    @Transactional
    public void removeRole(Long id) {
        Role role = mapper.selectById(id);
        if (role != null && "super_admin".equals(role.getRoleCode())) {
            throw new BusinessException("超级管理员角色不可删除");
        }
        mapper.deleteById(id);
    }

    /** 分配权限：先删后插 */
    @Transactional
    public void grant(Long roleId, List<Long> permissionIds) {
        rolePermissionMapper.delete(Wrappers.<RolePermission>lambdaQuery()
                .eq(RolePermission::getRoleId, roleId));
        if (permissionIds != null) {
            for (Long pid : permissionIds) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(pid);
                rolePermissionMapper.insert(rp);
            }
        }
    }

    public List<Long> permissionIdsOf(Long roleId) {
        return rolePermissionMapper.selectList(Wrappers.<RolePermission>lambdaQuery()
                        .eq(RolePermission::getRoleId, roleId))
                .stream().map(RolePermission::getPermissionId).collect(Collectors.toList());
    }
}
