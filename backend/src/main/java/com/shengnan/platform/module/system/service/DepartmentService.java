package com.shengnan.platform.module.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shengnan.platform.common.BusinessException;
import com.shengnan.platform.common.CrudService;
import com.shengnan.platform.module.system.entity.Department;
import com.shengnan.platform.module.system.mapper.DepartmentMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService extends CrudService<DepartmentMapper, Department> {
    private final DepartmentMapper mapper;
    public DepartmentService(DepartmentMapper mapper) { this.mapper = mapper; }
    @Override protected DepartmentMapper mapper() { return mapper; }

    public List<Department> search() {
        return mapper.selectList(Wrappers.<Department>lambdaQuery().orderByDesc(Department::getId));
    }

    /** 按 parentId 组装树（供左侧组织树 / 部门表格使用） */
    public java.util.List<java.util.Map<String, Object>> tree() {
        List<Department> all = mapper.selectList(Wrappers.<Department>lambdaQuery()
                .orderByAsc(Department::getSortNo).orderByAsc(Department::getId));
        java.util.Map<Long, List<Department>> byParent = new java.util.HashMap<>();
        for (Department d : all) {
            Long pid = d.getParentId() == null ? 0L : d.getParentId();
            byParent.computeIfAbsent(pid, k -> new java.util.ArrayList<>()).add(d);
        }
        return buildChildren(0L, byParent);
    }

    private java.util.List<java.util.Map<String, Object>> buildChildren(
            Long parentId, java.util.Map<Long, List<Department>> byParent) {
        java.util.List<java.util.Map<String, Object>> out = new java.util.ArrayList<>();
        for (Department d : byParent.getOrDefault(parentId, java.util.Collections.emptyList())) {
            java.util.Map<String, Object> node = new java.util.LinkedHashMap<>();
            node.put("id", d.getId());
            node.put("parentId", d.getParentId());
            node.put("deptName", d.getDeptName());
            node.put("deptCode", d.getDeptCode());
            node.put("leaderName", d.getLeaderName());
            node.put("contact", d.getContact());
            node.put("sortNo", d.getSortNo());
            node.put("isEnabled", d.getIsEnabled());
            java.util.List<java.util.Map<String, Object>> children = buildChildren(d.getId(), byParent);
            if (!children.isEmpty()) node.put("children", children);
            out.add(node);
        }
        return out;
    }

    public void remove(Long id) {
        Long children = mapper.selectCount(Wrappers.<Department>lambdaQuery()
                .eq(Department::getParentId, id));
        if (children != null && children > 0) throw new BusinessException("存在下级部门，请先删除下级");
        if (mapper.deleteById(id) == 0) throw new BusinessException("记录不存在");
    }
}
