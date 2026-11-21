package com.shengnan.platform.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** 登录主体：账号ID + 角色 + 权限集合 + 数据范围 */
@Data
public class LoginPrincipal implements UserDetails {
    private Long accountId;
    private String username;
    private String password;
    private boolean enabled = true;
    private List<String> roles;
    private List<String> permissions;
    private String dataScope;
    private Long departmentId;

    public boolean isSuperAdmin() {
        return roles != null && roles.contains("super_admin");
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (permissions == null) return Collections.emptyList();
        return permissions.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }
}
