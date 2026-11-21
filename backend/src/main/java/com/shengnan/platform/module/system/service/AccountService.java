package com.shengnan.platform.module.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shengnan.platform.common.BusinessException;
import com.shengnan.platform.common.CrudService;
import com.shengnan.platform.module.system.entity.*;
import com.shengnan.platform.module.system.mapper.*;
import com.shengnan.platform.security.LoginPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AccountService extends CrudService<AccountMapper, Account> {
    private final AccountMapper mapper;
    private final AccountRoleMapper accountRoleMapper;
    private final RoleMapper roleMapper;
    private final DepartmentMapper departmentMapper;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountMapper mapper, AccountRoleMapper accountRoleMapper,
                          RoleMapper roleMapper, DepartmentMapper departmentMapper,
                          PasswordEncoder passwordEncoder) {
        this.mapper = mapper;
        this.accountRoleMapper = accountRoleMapper;
        this.roleMapper = roleMapper;
        this.departmentMapper = departmentMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override protected AccountMapper mapper() { return mapper; }

    /** 数据权限过滤分页：all=全部, dept=本部门, dept_tree=本部门及以下, self=仅本人 */
    public IPage<Account> pageWithScope(int pageNum, int pageSize, String keyword) {
        LoginPrincipal me = currentUser();
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Account> q =
                Wrappers.<Account>lambdaQuery();
        if (keyword != null && !keyword.trim().isEmpty()) {
            q.and(w -> w.like(Account::getUsername, keyword)
                    .or().like(Account::getDisplayName, keyword));
        }
        applyScope(q, me, "created_by");
        return mapper.selectPage(new Page<>(pageNum, pageSize), q.orderByDesc(Account::getId));
    }

    public List<Account> listWithScope() {
        LoginPrincipal me = currentUser();
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Account> q =
                Wrappers.<Account>lambdaQuery();
        applyScope(q, me, "created_by");
        return mapper.selectList(q.orderByDesc(Account::getId));
    }

    private void applyScope(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Account> q,
                            LoginPrincipal me, String createdByField) {
        if (me.isSuperAdmin()) return;
        String scope = me.getDataScope() == null ? "self" : me.getDataScope();
        if ("self".equals(scope)) {
            q.eq(Account::getId, me.getAccountId());
        } else if ("dept".equals(scope) && me.getDepartmentId() != null) {
            q.eq(Account::getDepartmentId, me.getDepartmentId());
        } else if ("dept_tree".equals(scope) && me.getDepartmentId() != null) {
            List<Long> ids = departmentTree(me.getDepartmentId());
            q.in(Account::getDepartmentId, ids);
        }
    }

    private List<Long> departmentTree(Long rootId) {
        List<Long> result = new ArrayList<>();
        collectDepartments(rootId, result);
        return result.isEmpty() ? Collections.singletonList(rootId) : result;
    }

    private void collectDepartments(Long parentId, List<Long> out) {
        out.add(parentId);
        List<Department> children = departmentMapper.selectList(
                Wrappers.<Department>lambdaQuery().eq(Department::getParentId, parentId));
        for (Department d : children) collectDepartments(d.getId(), out);
    }

    @Transactional
    public void createAccount(Account account, List<Long> roleIds) {
        if (mapper.selectCount(Wrappers.<Account>lambdaQuery()
                .eq(Account::getUsername, account.getUsername())) > 0) {
            throw new BusinessException("账号已存在: " + account.getUsername());
        }
        account.setId(null);
        account.setPasswordHash(passwordEncoder.encode(
                account.getPasswordHash() == null ? "123456" : account.getPasswordHash()));
        mapper.insert(account);
        bindRoles(account.getId(), roleIds);
    }

    @Transactional
    public void updateAccount(Account account, List<Long> roleIds) {
        Account old = mapper.selectById(account.getId());
        if (old == null) throw new BusinessException("账号不存在");
        account.setUsername(null);      // 不允许改登录账号
        account.setPasswordHash(null);  // 密码走单独的重置接口
        mapper.updateById(account);
        if (roleIds != null) {
            accountRoleMapper.delete(Wrappers.<AccountRole>lambdaQuery()
                    .eq(AccountRole::getAccountId, account.getId()));
            bindRoles(account.getId(), roleIds);
        }
    }

    private void bindRoles(Long accountId, List<Long> roleIds) {
        if (roleIds == null) return;
        for (Long roleId : roleIds) {
            AccountRole ar = new AccountRole();
            ar.setAccountId(accountId);
            ar.setRoleId(roleId);
            accountRoleMapper.insert(ar);
        }
    }

    public void resetPassword(Long accountId, String newPassword) {
        Account a = mapper.selectById(accountId);
        if (a == null) throw new BusinessException("账号不存在");
        a.setPasswordHash(passwordEncoder.encode(newPassword == null ? "123456" : newPassword));
        mapper.updateById(a);
    }

    public List<Long> roleIdsOf(Long accountId) {
        return accountRoleMapper.selectList(Wrappers.<AccountRole>lambdaQuery()
                        .eq(AccountRole::getAccountId, accountId))
                .stream().map(AccountRole::getRoleId).collect(Collectors.toList());
    }

    public List<String> roleCodesOf(Long accountId) {
        List<Long> ids = roleIdsOf(accountId);
        if (ids.isEmpty()) return Collections.emptyList();
        return roleMapper.selectBatchIds(ids).stream().map(Role::getRoleCode).collect(Collectors.toList());
    }

    public Map<String, Object> options() {
        Map<String, Object> m = new HashMap<>();
        m.put("roles", roleMapper.selectList(Wrappers.<Role>lambdaQuery().eq(Role::getIsEnabled, true)));
        m.put("departments", departmentMapper.selectList(
                Wrappers.<Department>lambdaQuery().eq(Department::getIsEnabled, true).orderByAsc(Department::getSortNo)));
        return m;
    }

    private LoginPrincipal currentUser() {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (LoginPrincipal) p;
    }
}
