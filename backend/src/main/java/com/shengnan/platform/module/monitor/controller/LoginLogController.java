package com.shengnan.platform.module.monitor.controller;

import com.shengnan.platform.common.ApiResult;
import com.shengnan.platform.common.PageResult;
import com.shengnan.platform.module.monitor.entity.LoginLog;
import com.shengnan.platform.module.monitor.service.LoginLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/login-log")
public class LoginLogController {
    private final LoginLogService service;
    public LoginLogController(LoginLogService service) { this.service = service; }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('login-log:list')")
    public ApiResult<?> list(@RequestParam(defaultValue="1") int pageNum,
                             @RequestParam(defaultValue="1000") int pageSize) {
        return ApiResult.ok(PageResult.of(service.page(pageNum, pageSize)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('login-log:list')")
    public ApiResult<LoginLog> get(@PathVariable Long id) { return ApiResult.ok(service.getById(id)); }

    @PostMapping
    @PreAuthorize("hasAuthority('login-log:create') or hasAuthority('login-log:write')")
    public ApiResult<Void> create(@RequestBody LoginLog body) { service.create(body); return ApiResult.ok(); }

    @PutMapping
    @PreAuthorize("hasAuthority('login-log:update') or hasAuthority('login-log:write')")
    public ApiResult<Void> update(@RequestBody LoginLog body) { service.update(body); return ApiResult.ok(); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('login-log:delete') or hasAuthority('login-log:write')")
    public ApiResult<Void> delete(@PathVariable Long id) { service.remove(id); return ApiResult.ok(); }

    @DeleteMapping("/clean")
    @PreAuthorize("hasAuthority('login-log:clean')")
    public ApiResult<Void> clean() { service.cleanAll(); return ApiResult.ok(); }
}
