package com.shengnan.platform.module.system.controller;

import com.shengnan.platform.common.ApiResult;
import com.shengnan.platform.common.PageResult;
import com.shengnan.platform.module.system.entity.Notice;
import com.shengnan.platform.module.system.service.NoticeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 全员通知：管理端需 notice:* 权限；用户端查看/已读仅需登录 */
@RestController
@RequestMapping("/api/notice")
public class NoticeController {
    private final NoticeService service;

    public NoticeController(NoticeService service) { this.service = service; }

    /* ---------- 用户端：所有登录账号可见 ---------- */

    /** 已发布通知列表（带当前账号已读标记） */
    @GetMapping("/published")
    public ApiResult<List<Notice>> published() { return ApiResult.ok(service.published()); }

    /** 未读数量 */
    @GetMapping("/unread-count")
    public ApiResult<Integer> unreadCount() { return ApiResult.ok(service.unreadCount()); }

    /** 标记单条已读 */
    @PostMapping("/read/{id}")
    public ApiResult<Void> read(@PathVariable Long id) { service.markRead(id); return ApiResult.ok(); }

    /** 全部标记已读 */
    @PostMapping("/read-all")
    public ApiResult<Void> readAll() { service.markAllRead(); return ApiResult.ok(); }

    /* ---------- 管理端：需对应权限 ---------- */

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('notice:list')")
    public ApiResult<PageResult<Notice>> list(@RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize,
                                              @RequestParam(required = false) Integer status,
                                              @RequestParam(required = false) String title) {
        return ApiResult.ok(service.page(pageNum, pageSize, status, title));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('notice:list')")
    public ApiResult<Notice> get(@PathVariable Long id) { return ApiResult.ok(service.get(id)); }

    @PostMapping
    @PreAuthorize("hasAuthority('notice:create')")
    public ApiResult<Void> create(@RequestBody Notice body) { service.create(body); return ApiResult.ok(); }

    @PutMapping
    @PreAuthorize("hasAuthority('notice:update')")
    public ApiResult<Void> update(@RequestBody Notice body) { service.update(body); return ApiResult.ok(); }

    @PutMapping("/status/{id}")
    @PreAuthorize("hasAuthority('notice:publish')")
    public ApiResult<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        int status = body.get("status") == null ? 0 : body.get("status");
        service.changeStatus(id, status);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('notice:delete')")
    public ApiResult<Void> delete(@PathVariable Long id) { service.remove(id); return ApiResult.ok(); }
}
