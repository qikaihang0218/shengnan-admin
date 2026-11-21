package com.shengnan.platform.module.system.controller;

import com.shengnan.platform.common.ApiResult;
import com.shengnan.platform.module.system.entity.Role;
import com.shengnan.platform.module.system.service.RoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/role")
public class RoleController {
    private final RoleService service;

    public RoleController(RoleService service) { this.service = service; }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('role:list')")
    public ApiResult<List<Role>> list() { return ApiResult.ok(service.listAllEnabled()); }

    public static class RolePayload {
        public Role role;
        public List<Long> permissionIds;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('role:create')")
    public ApiResult<Void> create(@RequestBody RolePayload body) {
        service.createRole(body.role);
        if (body.permissionIds != null) service.grant(body.role.getId(), body.permissionIds);
        return ApiResult.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('role:update')")
    public ApiResult<Void> update(@RequestBody RolePayload body) {
        service.updateRole(body.role);
        if (body.permissionIds != null) service.grant(body.role.getId(), body.permissionIds);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    public ApiResult<Void> delete(@PathVariable Long id) { service.removeRole(id); return ApiResult.ok(); }

    @GetMapping("/permissions/{roleId}")
    @PreAuthorize("hasAuthority('role:list')")
    public ApiResult<List<Long>> permissions(@PathVariable Long roleId) {
        return ApiResult.ok(service.permissionIdsOf(roleId));
    }

    @PutMapping("/grant/{roleId}")
    @PreAuthorize("hasAuthority('role:grant')")
    public ApiResult<Void> grant(@PathVariable Long roleId, @RequestBody Map<String, List<Long>> body) {
        service.grant(roleId, body.get("permissionIds"));
        return ApiResult.ok();
    }
}
