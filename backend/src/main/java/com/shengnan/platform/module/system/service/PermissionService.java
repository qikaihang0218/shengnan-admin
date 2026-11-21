package com.shengnan.platform.module.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shengnan.platform.common.BusinessException;
import com.shengnan.platform.common.CrudService;
import com.shengnan.platform.module.system.entity.Permission;
import com.shengnan.platform.module.system.mapper.PermissionMapper;
import com.shengnan.platform.security.LoginPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PermissionService extends CrudService<PermissionMapper, Permission> {
    private final PermissionMapper mapper;

    public PermissionService(PermissionMapper mapper) { this.mapper = mapper; }

    @Override protected PermissionMapper mapper() { return mapper; }

    /** 构建前端路由树：只取目录/菜单，按当前用户权限过滤 */
    public List<Map<String, Object>> routeTree() {
        LoginPrincipal me = (LoginPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        List<Permission> all = mapper.selectList(Wrappers.<Permission>lambdaQuery()
                .eq(Permission::getIsEnabled, true)
                .orderByAsc(Permission::getSortNo));
        List<Permission> navs = all.stream()
                .filter(p -> "catalog".equals(p.getResourceType()) || "menu".equals(p.getResourceType()))
                .filter(p -> me.isSuperAdmin() || me.getPermissions().contains(p.getPermission())
                        || (p.getPermission() == null || p.getPermission().isEmpty()))
                .collect(Collectors.toList());
        return buildTree(navs, 0L);
    }

    /** 完整权限树（含按钮），用于角色授权勾选 */
    public List<Map<String, Object>> fullTree() {
        List<Permission> all = mapper.selectList(Wrappers.<Permission>lambdaQuery()
                .orderByAsc(Permission::getSortNo));
        return buildTree(all, 0L);
    }

    private List<Map<String, Object>> buildTree(List<Permission> source, Long parentId) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Permission p : source) {
            if (Objects.equals(p.getParentId(), parentId)) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("id", p.getId());
                node.put("parentId", p.getParentId());
                node.put("name", p.getResourceName());
                node.put("type", p.getResourceType());
                node.put("path", p.getRoutePath());
                node.put("component", p.getComponent());
                node.put("icon", p.getIconName());
                node.put("permission", p.getPermission());
                node.put("visible", p.getIsVisible());
                node.put("sortNo", p.getSortNo());
                List<Map<String, Object>> children = buildTree(source, p.getId());
                if (!children.isEmpty()) node.put("children", children);
                nodes.add(node);
            }
        }
        return nodes;
    }

    public void createPermission(Permission p) {
        p.setId(null);
        mapper.insert(p);
    }

    public void updatePermission(Permission p) {
        if (mapper.selectById(p.getId()) == null) throw new BusinessException("资源不存在");
        mapper.updateById(p);
    }

    public void removePermission(Long id) {
        Long children = mapper.selectCount(Wrappers.<Permission>lambdaQuery().eq(Permission::getParentId, id));
        if (children != null && children > 0) throw new BusinessException("存在子资源，不可删除");
        mapper.deleteById(id);
    }
}
