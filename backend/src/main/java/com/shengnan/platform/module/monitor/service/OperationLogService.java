package com.shengnan.platform.module.monitor.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shengnan.platform.common.BusinessException;
import com.shengnan.platform.common.CrudService;
import com.shengnan.platform.module.monitor.entity.OperationLog;
import com.shengnan.platform.module.monitor.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OperationLogService extends CrudService<OperationLogMapper, OperationLog> {
    private final OperationLogMapper mapper;
    public OperationLogService(OperationLogMapper mapper) { this.mapper = mapper; }
    @Override protected OperationLogMapper mapper() { return mapper; }

    public List<OperationLog> search() {
        return mapper.selectList(Wrappers.<OperationLog>lambdaQuery().orderByDesc(OperationLog::getId));
    }

    public void remove(Long id) {
        if (mapper.deleteById(id) == 0) throw new BusinessException("记录不存在");
    }

    public void cleanAll() {
        mapper.delete(com.baomidou.mybatisplus.core.toolkit.Wrappers.<com.shengnan.platform.module.monitor.entity.OperationLog>lambdaQuery()
                .isNotNull(com.shengnan.platform.module.monitor.entity.OperationLog::getId));
    }
}
