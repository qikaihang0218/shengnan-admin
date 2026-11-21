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

    public void remove(Long id) {
        if (mapper.deleteById(id) == 0) throw new BusinessException("记录不存在");
    }
}
