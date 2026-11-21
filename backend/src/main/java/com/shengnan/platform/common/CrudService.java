package com.shengnan.platform.common;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/** 通用 CRUD 服务：各业务服务继承后自动拥有基础能力 */
public abstract class CrudService<M extends BaseMapper<T>, T> {

    protected abstract M mapper();

    public T getById(Long id) {
        return mapper().selectById(id);
    }

    public List<T> listAll() {
        return mapper().selectList(null);
    }

    public IPage<T> page(int pageNum, int pageSize) {
        return mapper().selectPage(new Page<>(pageNum, pageSize), null);
    }

    public int create(T entity) {
        return mapper().insert(entity);
    }

    public int update(T entity) {
        return mapper().updateById(entity);
    }

    public int deleteById(Long id) {
        return mapper().deleteById(id);
    }

    public int deleteByIds(List<Long> ids) {
        return mapper().deleteBatchIds(ids);
    }
}
