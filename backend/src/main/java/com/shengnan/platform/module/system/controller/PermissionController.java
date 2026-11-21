package com.shengnan.platform.module.system.controller;

import com.shengnan.platform.common.ApiResult;
import com.shengnan.platform.module.system.entity.Permission;
import com.shengnan.platform.module.system.service.PermissionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/permission")
public class PermissionController {
    private final PermissionService service;

    public PermissionController(PermissionService service) { this.service = service; }

    @GetMapping("/routes")
    public ApiResult<List<Map<String, Object>>> routes() { return ApiResult.ok(service.routeTree()); }

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('permission:list')")
    public ApiResult<List<Map<String, Object>>> tree() { return ApiResult.ok(service.fullTree()); }

    @PostMapping
    @PreAuthorize("hasAuthority('permission:create')")
    public ApiResult<Void> create(@RequestBody Permission body) { service.createPermission(body); return ApiResult.ok(); }

    @PutMapping
    @PreAuthorize("hasAuthority('permission:update')")
    public ApiResult<Void> update(@RequestBody Permission body) { service.updatePermission(body); return ApiResult.ok(); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:delete')")
    public ApiResult<Void> delete(@PathVariable Long id) { service.removePermission(id); return ApiResult.ok(); }
}
