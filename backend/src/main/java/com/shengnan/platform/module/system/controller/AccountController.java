package com.shengnan.platform.module.system.controller;

import com.shengnan.platform.common.ApiResult;
import com.shengnan.platform.common.PageResult;
import com.shengnan.platform.module.system.entity.Account;
import com.shengnan.platform.module.system.service.AccountService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final AccountService service;

    public AccountController(AccountService service) { this.service = service; }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('account:list')")
    public ApiResult<PageResult<Account>> list(@RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize,
                                               @RequestParam(required = false) String keyword) {
        return ApiResult.ok(PageResult.of(service.pageWithScope(pageNum, pageSize, keyword)));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAuthority('account:list')")
    public ApiResult<Map<String, Object>> options() {
        return ApiResult.ok(service.options());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('account:list')")
    public ApiResult<Map<String, Object>> get(@PathVariable Long id) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("account", service.getById(id));
        m.put("roleIds", service.roleIdsOf(id));
        return ApiResult.ok(m);
    }

    public static class AccountPayload {
        public Account account;
        public List<Long> roleIds;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('account:create')")
    public ApiResult<Void> create(@RequestBody AccountPayload body) {
        service.createAccount(body.account, body.roleIds);
        return ApiResult.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('account:update')")
    public ApiResult<Void> update(@RequestBody AccountPayload body) {
        service.updateAccount(body.account, body.roleIds);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('account:delete')")
    public ApiResult<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ApiResult.ok();
    }

    @PutMapping("/password/{id}")
    @PreAuthorize("hasAuthority('account:reset-pwd')")
    public ApiResult<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        service.resetPassword(id, body.get("password"));
        return ApiResult.ok();
    }
}
