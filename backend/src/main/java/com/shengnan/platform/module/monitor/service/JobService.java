package com.shengnan.platform.module.monitor.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shengnan.platform.common.BusinessException;
import com.shengnan.platform.common.PageResult;
import com.shengnan.platform.module.monitor.entity.Job;
import com.shengnan.platform.module.monitor.entity.JobLog;
import com.shengnan.platform.module.monitor.mapper.JobLogMapper;
import com.shengnan.platform.module.monitor.mapper.JobMapper;
import com.shengnan.platform.security.LoginPrincipal;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobService {
    private final JobMapper mapper;
    private final JobLogMapper logMapper;
    private final JobExecutor executor;
    private final JobScheduler scheduler;

    public JobService(JobMapper mapper, JobLogMapper logMapper, JobExecutor executor,
                      @Lazy JobScheduler scheduler) {
        this.mapper = mapper;
        this.logMapper = logMapper;
        this.executor = executor;
        this.scheduler = scheduler;
    }

    public PageResult<Job> page(int pageNum, int pageSize) {
        IPage<Job> p = mapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<Job>lambdaQuery().orderByDesc(Job::getId));
        return PageResult.of(p);
    }

    public Job get(Long id) {
        Job j = mapper.selectById(id);
        if (j == null) throw new BusinessException("任务不存在");
        return j;
    }

    public void create(Job job) {
        requireScriptPermIfNeeded(job);
        executor.validate(job);
        job.setId(null);
        if (job.getIsEnabled() == null) job.setIsEnabled(true);
        mapper.insert(job);
        if (Boolean.TRUE.equals(job.getIsEnabled())) scheduler.register(job);
    }

    public void update(Job job) {
        if (job.getId() == null) throw new BusinessException("任务ID不能为空");
        requireScriptPermIfNeeded(job);
        executor.validate(job);
        Job old = mapper.selectById(job.getId());
        if (old == null) throw new BusinessException("任务不存在");
        scheduler.cancel(job.getId());
        mapper.updateById(job);
        Job fresh = mapper.selectById(job.getId());
        if (Boolean.TRUE.equals(fresh.getIsEnabled())) scheduler.register(fresh);
    }

    public void delete(Long id) {
        scheduler.cancel(id);
        mapper.deleteById(id);
    }

    public void toggle(Long id, Boolean enabled) {
        Job job = get(id);
        if (Boolean.TRUE.equals(enabled)) {
            requireScriptPermIfNeeded(job);
            executor.validate(job);
        }
        job.setIsEnabled(enabled);
        mapper.updateById(job);
        if (Boolean.TRUE.equals(enabled)) scheduler.register(job);
        else scheduler.cancel(id);
    }

    public String runOnce(Long id) {
        Job job = get(id);
        requireScriptPermIfNeeded(job);
        return scheduler.runOnce(id);
    }

    public PageResult<JobLog> logPage(int pageNum, int pageSize, Long jobId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<JobLog> q =
                Wrappers.<JobLog>lambdaQuery();
        if (jobId != null) q.eq(JobLog::getJobId, jobId);
        IPage<JobLog> p = logMapper.selectPage(new Page<>(pageNum, pageSize), q.orderByDesc(JobLog::getFiredAt));
        return PageResult.of(p);
    }

    public void cleanLogs() {
        logMapper.delete(Wrappers.<JobLog>lambdaQuery().isNotNull(JobLog::getId));
    }

    /** Shell/Java 脚本任务需 job:script 权限；bean 类型只需普通任务权限 */
    private void requireScriptPermIfNeeded(Job job) {
        String kind = job.getJobKind();
        if ("shell".equals(kind) || "java".equals(kind)) {
            LoginPrincipal me = (LoginPrincipal) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            if (!me.isSuperAdmin() && !me.getPermissions().contains("job:script")) {
                throw new BusinessException(403, "需要 job:script 权限才能配置或执行脚本任务");
            }
        }
    }

    public List<String> beanTargets() {
        return java.util.Arrays.asList(
                "systemTasks.cleanBlocklist()",
                "systemTasks.heartbeat()"
        );
    }
}
