package com.shengnan.platform.module.system.controller;

import com.shengnan.platform.common.ApiResult;
import com.shengnan.platform.module.system.entity.Config;
import com.shengnan.platform.module.system.service.ConfigService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
public class ConfigController {
    private final ConfigService service;
    public ConfigController(ConfigService service) { this.service = service; }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('config:list')")
    public ApiResult<?> list(@RequestParam(defaultValue="1") int pageNum,
                             @RequestParam(defaultValue="1000") int pageSize) {
        return ApiResult.ok(service.search());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('config:list')")
    public ApiResult<Config> get(@PathVariable Long id) { return ApiResult.ok(service.getById(id)); }

    @PostMapping
    @PreAuthorize("hasAuthority('config:create') or hasAuthority('config:write')")
    public ApiResult<Void> create(@RequestBody Config body) { service.create(body); return ApiResult.ok(); }

    @PutMapping
    @PreAuthorize("hasAuthority('config:update') or hasAuthority('config:write')")
    public ApiResult<Void> update(@RequestBody Config body) { service.update(body); return ApiResult.ok(); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('config:delete') or hasAuthority('config:write')")
    public ApiResult<Void> delete(@PathVariable Long id) { service.remove(id); return ApiResult.ok(); }
}
