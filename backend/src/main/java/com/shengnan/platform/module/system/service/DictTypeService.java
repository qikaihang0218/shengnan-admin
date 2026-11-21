package com.shengnan.platform.module.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shengnan.platform.common.BusinessException;
import com.shengnan.platform.common.CrudService;
import com.shengnan.platform.module.system.entity.DictType;
import com.shengnan.platform.module.system.mapper.DictTypeMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DictTypeService extends CrudService<DictTypeMapper, DictType> {
    private final DictTypeMapper mapper;
    public DictTypeService(DictTypeMapper mapper) { this.mapper = mapper; }
    @Override protected DictTypeMapper mapper() { return mapper; }

    public List<DictType> search() {
        return mapper.selectList(Wrappers.<DictType>lambdaQuery().orderByDesc(DictType::getId));
    }

    public void remove(Long id) {
        if (mapper.deleteById(id) == 0) throw new BusinessException("记录不存在");
    }
}
