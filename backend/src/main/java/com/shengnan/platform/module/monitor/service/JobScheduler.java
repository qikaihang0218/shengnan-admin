package com.shengnan.platform.module.monitor.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shengnan.platform.common.BusinessException;
import com.shengnan.platform.module.monitor.entity.Job;
import com.shengnan.platform.module.monitor.mapper.JobMapper;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/** 动态调度器：启动加载启用任务，支持运行时增删改启停 */
@Component
public class JobScheduler implements SchedulingConfigurer, ApplicationListener<ApplicationReadyEvent> {
    private final JobMapper jobMapper;
    private final JobExecutor jobExecutor;
    private ScheduledTaskRegistrar registrar;
    private final Map<Long, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    public JobScheduler(JobMapper jobMapper, JobExecutor jobExecutor) {
        this.jobMapper = jobMapper;
        this.jobExecutor = jobExecutor;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        this.registrar = registrar;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        reloadAll();
    }

    public synchronized void reloadAll() {
        if (registrar == null || registrar.getScheduler() == null) return;
        for (ScheduledFuture<?> f : futures.values()) f.cancel(false);
        futures.clear();
        List<Job> jobs = jobMapper.selectList(Wrappers.<Job>lambdaQuery().eq(Job::getIsEnabled, true));
        for (Job job : jobs) {
            try { register(job); }
            catch (Exception ignored) { /* 非法 cron 跳过，避免启动失败 */ }
        }
    }

    public synchronized void register(Job job) {
        if (registrar == null || registrar.getScheduler() == null) return;
        if (!Boolean.TRUE.equals(job.getIsEnabled())) return;
        Runnable task = () -> {
            Job fresh = jobMapper.selectById(job.getId());
            if (fresh != null && Boolean.TRUE.equals(fresh.getIsEnabled())) {
                jobExecutor.run(fresh, "自动调度");
            }
        };
        ScheduledFuture<?> future = registrar.getScheduler().schedule(
                task, new CronTrigger(job.getCronExpr()));
        futures.put(job.getId(), future);
    }

    public synchronized void cancel(Long jobId) {
        ScheduledFuture<?> f = futures.remove(jobId);
        if (f != null) f.cancel(false);
    }

    /** 立即执行一次 */
    public String runOnce(Long jobId) {
        Job job = jobMapper.selectById(jobId);
        if (job == null) throw new BusinessException("任务不存在");
        return jobExecutor.run(job, "手动执行");
    }
}
