package com.shengnan.platform.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shengnan.platform.common.BusinessException;
import com.shengnan.platform.module.system.entity.*;
import com.shengnan.platform.module.system.mapper.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/** 加载账号的角色/权限/数据范围，构造登录主体 */
@Service
public class UserDetailsServiceImpl implements org.springframework.security.core.userdetails.UserDetailsService {

    private final AccountMapper accountMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final AccountRoleMapper accountRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;

    public UserDetailsServiceImpl(AccountMapper accountMapper, RoleMapper roleMapper,
                                  PermissionMapper permissionMapper, AccountRoleMapper accountRoleMapper,
                                  RolePermissionMapper rolePermissionMapper) {
        this.accountMapper = accountMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.accountRoleMapper = accountRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    @Override
    public LoginPrincipal loadUserByUsername(String username) {
        Account account = accountMapper.selectOne(Wrappers.<Account>lambdaQuery()
                .eq(Account::getUsername, username));
        if (account == null) throw new BusinessException("账号不存在: " + username);
        if (Boolean.FALSE.equals(account.getIsEnabled())) throw new BusinessException("账号已停用");

        List<Long> roleIds = accountRoleMapper.selectList(Wrappers.<AccountRole>lambdaQuery()
                        .eq(AccountRole::getAccountId, account.getId()))
                .stream().map(AccountRole::getRoleId).collect(Collectors.toList());

        List<Role> roles = roleIds.isEmpty() ? java.util.Collections.emptyList()
                : roleMapper.selectBatchIds(roleIds);
        List<String> roleCodes = roles.stream().map(Role::getRoleCode).collect(Collectors.toList());

        List<String> perms;
        if (roleCodes.contains("super_admin")) {
            perms = permissionMapper.selectList(Wrappers.<Permission>lambdaQuery()
                            .eq(Permission::getIsEnabled, true))
                    .stream().map(Permission::getPermission)
                    .filter(p -> p != null && !p.isEmpty()).distinct().collect(Collectors.toList());
        } else if (roleIds.isEmpty()) {
            perms = java.util.Collections.emptyList();
        } else {
            List<Long> permIds = rolePermissionMapper.selectList(Wrappers.<RolePermission>lambdaQuery()
                            .in(RolePermission::getRoleId, roleIds))
                    .stream().map(RolePermission::getPermissionId).collect(Collectors.toList());
            perms = permIds.isEmpty() ? java.util.Collections.emptyList()
                    : permissionMapper.selectBatchIds(permIds).stream()
                    .map(Permission::getPermission)
                    .filter(p -> p != null && !p.isEmpty()).distinct().collect(Collectors.toList());
        }

        String dataScope = roles.isEmpty() ? "self" : roles.get(0).getDataScope();
        if (roleCodes.contains("super_admin")) dataScope = "all";

        LoginPrincipal principal = new LoginPrincipal();
        principal.setAccountId(account.getId());
        principal.setUsername(account.getUsername());
        principal.setPassword(account.getPasswordHash());
        principal.setEnabled(Boolean.TRUE.equals(account.getIsEnabled()));
        principal.setRoles(roleCodes);
        principal.setPermissions(perms);
        principal.setDataScope(dataScope);
        principal.setDepartmentId(account.getDepartmentId());
        return principal;
    }
}
