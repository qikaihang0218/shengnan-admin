package com.shengnan.platform.module.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shengnan.platform.common.BusinessException;
import com.shengnan.platform.common.CrudService;
import com.shengnan.platform.module.system.entity.DictItem;
import com.shengnan.platform.module.system.mapper.DictItemMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DictItemService extends CrudService<DictItemMapper, DictItem> {
    private final DictItemMapper mapper;
    public DictItemService(DictItemMapper mapper) { this.mapper = mapper; }
    @Override protected DictItemMapper mapper() { return mapper; }

    public List<DictItem> search() {
        return mapper.selectList(Wrappers.<DictItem>lambdaQuery().orderByDesc(DictItem::getId));
    }

    public List<DictItem> searchByType(Long typeId) {
        return mapper.selectList(Wrappers.<DictItem>lambdaQuery()
                .eq(DictItem::getTypeId, typeId)
                .orderByAsc(DictItem::getSortNo)
                .orderByAsc(DictItem::getId));
    }

    public void removeByType(Long typeId) {
        mapper.delete(Wrappers.<DictItem>lambdaQuery().eq(DictItem::getTypeId, typeId));
    }

    public void remove(Long id) {
        if (mapper.deleteById(id) == 0) throw new BusinessException("记录不存在");
    }
}
