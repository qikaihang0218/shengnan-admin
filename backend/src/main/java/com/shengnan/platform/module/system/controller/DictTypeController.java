package com.shengnan.platform.module.system.controller;

import com.shengnan.platform.common.ApiResult;
import com.shengnan.platform.module.system.entity.DictType;
import com.shengnan.platform.module.system.service.DictTypeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dict")
public class DictTypeController {
    private final DictTypeService service;
    public DictTypeController(DictTypeService service) { this.service = service; }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('dict:list')")
    public ApiResult<?> list(@RequestParam(defaultValue="1") int pageNum,
                             @RequestParam(defaultValue="1000") int pageSize) {
        return ApiResult.ok(service.search());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('dict:list')")
    public ApiResult<DictType> get(@PathVariable Long id) { return ApiResult.ok(service.getById(id)); }

    @PostMapping
    @PreAuthorize("hasAuthority('dict:create') or hasAuthority('dict:write')")
    public ApiResult<Void> create(@RequestBody DictType body) { service.create(body); return ApiResult.ok(); }

    @PutMapping
    @PreAuthorize("hasAuthority('dict:update') or hasAuthority('dict:write')")
    public ApiResult<Void> update(@RequestBody DictType body) { service.update(body); return ApiResult.ok(); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('dict:delete') or hasAuthority('dict:write')")
    public ApiResult<Void> delete(@PathVariable Long id) { service.remove(id); return ApiResult.ok(); }
}
