package com.shengnan.platform.module.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shengnan.platform.common.BusinessException;
import com.shengnan.platform.common.CrudService;
import com.shengnan.platform.module.system.entity.Config;
import com.shengnan.platform.module.system.mapper.ConfigMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfigService extends CrudService<ConfigMapper, Config> {
    private final ConfigMapper mapper;
    public ConfigService(ConfigMapper mapper) { this.mapper = mapper; }
    @Override protected ConfigMapper mapper() { return mapper; }

    public List<Config> search() {
        return mapper.selectList(Wrappers.<Config>lambdaQuery().orderByDesc(Config::getId));
    }

    public void remove(Long id) {
        if (mapper.deleteById(id) == 0) throw new BusinessException("记录不存在");
    }
}
