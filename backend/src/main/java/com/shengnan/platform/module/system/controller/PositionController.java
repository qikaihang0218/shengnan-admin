package com.shengnan.platform.module.system.controller;

import com.shengnan.platform.common.ApiResult;
import com.shengnan.platform.module.system.entity.Position;
import com.shengnan.platform.module.system.service.PositionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/position")
public class PositionController {
    private final PositionService service;
    public PositionController(PositionService service) { this.service = service; }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('position:list')")
    public ApiResult<?> list(@RequestParam(defaultValue="1") int pageNum,
                             @RequestParam(defaultValue="1000") int pageSize) {
        return ApiResult.ok(service.search());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('position:list')")
    public ApiResult<Position> get(@PathVariable Long id) { return ApiResult.ok(service.getById(id)); }

    @PostMapping
    @PreAuthorize("hasAuthority('position:create') or hasAuthority('position:write')")
    public ApiResult<Void> create(@RequestBody Position body) { service.create(body); return ApiResult.ok(); }

    @PutMapping
    @PreAuthorize("hasAuthority('position:update') or hasAuthority('position:write')")
    public ApiResult<Void> update(@RequestBody Position body) { service.update(body); return ApiResult.ok(); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('position:delete') or hasAuthority('position:write')")
    public ApiResult<Void> delete(@PathVariable Long id) { service.remove(id); return ApiResult.ok(); }
}
