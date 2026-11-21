package com.shengnan.platform.module.monitor.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shengnan.platform.common.BusinessException;
import com.shengnan.platform.common.CrudService;
import com.shengnan.platform.module.monitor.entity.LoginLog;
import com.shengnan.platform.module.monitor.mapper.LoginLogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoginLogService extends CrudService<LoginLogMapper, LoginLog> {
    private final LoginLogMapper mapper;
    public LoginLogService(LoginLogMapper mapper) { this.mapper = mapper; }
    @Override protected LoginLogMapper mapper() { return mapper; }

    public List<LoginLog> search() {
        return mapper.selectList(Wrappers.<LoginLog>lambdaQuery().orderByDesc(LoginLog::getId));
    }

    public void remove(Long id) {
        if (mapper.deleteById(id) == 0) throw new BusinessException("记录不存在");
    }

    public void cleanAll() {
        mapper.delete(com.baomidou.mybatisplus.core.toolkit.Wrappers.<com.shengnan.platform.module.monitor.entity.LoginLog>lambdaQuery()
                .isNotNull(com.shengnan.platform.module.monitor.entity.LoginLog::getId));
    }
}
