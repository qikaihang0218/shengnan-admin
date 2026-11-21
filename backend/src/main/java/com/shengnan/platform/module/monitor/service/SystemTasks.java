package com.shengnan.platform.module.monitor.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shengnan.platform.module.monitor.entity.TokenBlocklist;
import com.shengnan.platform.module.monitor.mapper.TokenBlocklistMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 内置可被定时任务调用的 bean（调用目标 systemTasks.xxx()） */
@Component("systemTasks")
public class SystemTasks {
    private final TokenBlocklistMapper blocklistMapper;

    public SystemTasks(TokenBlocklistMapper blocklistMapper) {
        this.blocklistMapper = blocklistMapper;
    }

    /** 清理过期令牌黑名单 */
    public String cleanBlocklist() {
        Long n = blocklistMapper.selectCount(Wrappers.<TokenBlocklist>lambdaQuery()
                .le(TokenBlocklist::getExpiresAt, LocalDateTime.now()));
        if (n != null && n > 0) {
            blocklistMapper.delete(Wrappers.<TokenBlocklist>lambdaQuery()
                    .le(TokenBlocklist::getExpiresAt, LocalDateTime.now()));
        }
        return "清理过期令牌 " + (n == null ? 0 : n) + " 条";
    }

    /** 心跳测试任务 */
    public String heartbeat() {
        return "心跳正常 @" + LocalDateTime.now();
    }
}
