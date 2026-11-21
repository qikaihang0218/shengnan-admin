package com.shengnan.platform.module.monitor.controller;

import com.shengnan.platform.common.ApiResult;
import com.shengnan.platform.common.PageResult;
import com.shengnan.platform.module.monitor.entity.Job;
import com.shengnan.platform.module.monitor.entity.JobLog;
import com.shengnan.platform.module.monitor.service.JobService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/job")
public class JobController {
    private final JobService service;

    public JobController(JobService service) { this.service = service; }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('job:list')")
    public ApiResult<PageResult<Job>> list(@RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.ok(service.page(pageNum, pageSize));
    }

    @GetMapping("/targets")
    @PreAuthorize("hasAuthority('job:list')")
    public ApiResult<List<String>> targets() { return ApiResult.ok(service.beanTargets()); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('job:list')")
    public ApiResult<Job> get(@PathVariable Long id) { return ApiResult.ok(service.get(id)); }

    @PostMapping
    @PreAuthorize("hasAuthority('job:create')")
    public ApiResult<Void> create(@RequestBody Job body) { service.create(body); return ApiResult.ok(); }

    @PutMapping
    @PreAuthorize("hasAuthority('job:update')")
    public ApiResult<Void> update(@RequestBody Job body) { service.update(body); return ApiResult.ok(); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('job:delete')")
    public ApiResult<Void> delete(@PathVariable Long id) { service.delete(id); return ApiResult.ok(); }

    @PutMapping("/toggle/{id}")
    @PreAuthorize("hasAuthority('job:update')")
    public ApiResult<Void> toggle(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        service.toggle(id, body.get("enabled"));
        return ApiResult.ok();
    }

    @PostMapping("/run/{id}")
    @PreAuthorize("hasAuthority('job:run')")
    public ApiResult<String> run(@PathVariable Long id) { return ApiResult.ok(service.runOnce(id)); }

    @GetMapping("/log/list")
    @PreAuthorize("hasAuthority('job:log-list')")
    public ApiResult<PageResult<JobLog>> logs(@RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize,
                                              @RequestParam(required = false) Long jobId) {
        return ApiResult.ok(service.logPage(pageNum, pageSize, jobId));
    }

    @DeleteMapping("/log/clean")
    @PreAuthorize("hasAuthority('job:log-list')")
    public ApiResult<Void> cleanLogs() { service.cleanLogs(); return ApiResult.ok(); }
}
