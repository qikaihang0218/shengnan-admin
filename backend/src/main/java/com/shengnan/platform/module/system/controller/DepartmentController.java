package com.shengnan.platform.module.system.controller;

import com.shengnan.platform.common.ApiResult;
import com.shengnan.platform.module.system.entity.Department;
import com.shengnan.platform.module.system.service.DepartmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/department")
public class DepartmentController {
    private final DepartmentService service;
    public DepartmentController(DepartmentService service) { this.service = service; }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('department:list')")
    public ApiResult<?> list(@RequestParam(defaultValue="1") int pageNum,
                             @RequestParam(defaultValue="1000") int pageSize) {
        return ApiResult.ok(service.search());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('department:list')")
    public ApiResult<Department> get(@PathVariable Long id) { return ApiResult.ok(service.getById(id)); }

    @PostMapping
    @PreAuthorize("hasAuthority('department:create') or hasAuthority('department:write')")
    public ApiResult<Void> create(@RequestBody Department body) { service.create(body); return ApiResult.ok(); }

    @PutMapping
    @PreAuthorize("hasAuthority('department:update') or hasAuthority('department:write')")
    public ApiResult<Void> update(@RequestBody Department body) { service.update(body); return ApiResult.ok(); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('department:delete') or hasAuthority('department:write')")
    public ApiResult<Void> delete(@PathVariable Long id) { service.remove(id); return ApiResult.ok(); }
}
