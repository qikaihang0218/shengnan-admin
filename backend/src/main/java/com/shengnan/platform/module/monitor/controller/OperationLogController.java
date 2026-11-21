package com.shengnan.platform.module.monitor.controller;

import com.shengnan.platform.common.ApiResult;
import com.shengnan.platform.common.PageResult;
import com.shengnan.platform.module.monitor.entity.OperationLog;
import com.shengnan.platform.module.monitor.service.OperationLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operation-log")
public class OperationLogController {
    private final OperationLogService service;
    public OperationLogController(OperationLogService service) { this.service = service; }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('operation-log:list')")
    public ApiResult<?> list(@RequestParam(defaultValue="1") int pageNum,
                             @RequestParam(defaultValue="1000") int pageSize) {
        return ApiResult.ok(PageResult.of(service.page(pageNum, pageSize)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('operation-log:list')")
    public ApiResult<OperationLog> get(@PathVariable Long id) { return ApiResult.ok(service.getById(id)); }

    @PostMapping
    @PreAuthorize("hasAuthority('operation-log:create') or hasAuthority('operation-log:write')")
    public ApiResult<Void> create(@RequestBody OperationLog body) { service.create(body); return ApiResult.ok(); }

    @PutMapping
    @PreAuthorize("hasAuthority('operation-log:update') or hasAuthority('operation-log:write')")
    public ApiResult<Void> update(@RequestBody OperationLog body) { service.update(body); return ApiResult.ok(); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('operation-log:delete') or hasAuthority('operation-log:write')")
    public ApiResult<Void> delete(@PathVariable Long id) { service.remove(id); return ApiResult.ok(); }

    @DeleteMapping("/clean")
    @PreAuthorize("hasAuthority('operation-log:clean')")
    public ApiResult<Void> clean() { service.cleanAll(); return ApiResult.ok(); }
}
