package com.shengnan.platform.module.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shengnan.platform.common.BusinessException;
import com.shengnan.platform.common.CrudService;
import com.shengnan.platform.module.system.entity.Position;
import com.shengnan.platform.module.system.mapper.PositionMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PositionService extends CrudService<PositionMapper, Position> {
    private final PositionMapper mapper;
    public PositionService(PositionMapper mapper) { this.mapper = mapper; }
    @Override protected PositionMapper mapper() { return mapper; }

    public List<Position> search() {
        return mapper.selectList(Wrappers.<Position>lambdaQuery().orderByDesc(Position::getId));
    }

    public void remove(Long id) {
        if (mapper.deleteById(id) == 0) throw new BusinessException("记录不存在");
    }
}
