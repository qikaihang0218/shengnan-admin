package com.shengnan.platform.module.repair.controller;

import com.shengnan.platform.common.ApiResult;
import com.shengnan.platform.common.PageResult;
import com.shengnan.platform.module.repair.entity.RepairOrder;
import com.shengnan.platform.module.repair.service.RepairService;
import com.shengnan.platform.module.system.entity.Account;
import com.shengnan.platform.module.system.entity.DictItem;
import com.shengnan.platform.module.monitor.entity.FileObject;
import com.shengnan.platform.module.monitor.service.FileService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/** 报修工单：查询/提交/编辑/派单/处理/完工/关闭/评价 */
@RestController
@RequestMapping("/api/repair")
public class RepairController {

    private final RepairService service;
    private final FileService fileService;

    public RepairController(RepairService service, FileService fileService) {
        this.service = service;
        this.fileService = fileService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('repair:list')")
    public ApiResult<PageResult<RepairOrder>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                   @RequestParam(defaultValue = "10") int pageSize,
                                                   @RequestParam(required = false) Integer status,
                                                   @RequestParam(required = false) Integer priority,
                                                   @RequestParam(required = false) String category,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Boolean onlyMine) {
        return ApiResult.ok(service.page(pageNum, pageSize, status, priority, category, keyword, onlyMine));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('repair:list')")
    public ApiResult<Map<String, Object>> stats() { return ApiResult.ok(service.stats()); }

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('repair:list')")
    public ApiResult<List<DictItem>> categories() { return ApiResult.ok(service.categories()); }

    @GetMapping("/handlers")
    @PreAuthorize("hasAuthority('repair:list')")
    public ApiResult<List<Account>> handlers() { return ApiResult.ok(service.handlers()); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('repair:list')")
    public ApiResult<RepairOrder> get(@PathVariable Long id) { return ApiResult.ok(service.get(id)); }

    @PostMapping
    @PreAuthorize("hasAuthority('repair:create')")
    public ApiResult<Long> create(@RequestBody RepairOrder body) { return ApiResult.ok(service.create(body)); }

    @PutMapping
    @PreAuthorize("hasAuthority('repair:update')")
    public ApiResult<Void> update(@RequestBody RepairOrder body) { service.update(body); return ApiResult.ok(); }

    @PutMapping("/assign/{id}")
    @PreAuthorize("hasAuthority('repair:assign')")
    public ApiResult<Void> assign(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long handlerId = body.get("handlerId") == null ? null : Long.valueOf(body.get("handlerId").toString());
        service.assign(id, handlerId, parseTime(body.get("expectTime")));
        return ApiResult.ok();
    }

    @PutMapping("/start/{id}")
    @PreAuthorize("hasAuthority('repair:finish')")
    public ApiResult<Void> start(@PathVariable Long id) { service.start(id); return ApiResult.ok(); }

    @PutMapping("/finish/{id}")
    @PreAuthorize("hasAuthority('repair:finish')")
    public ApiResult<Void> finish(@PathVariable Long id, @RequestBody Map<String, String> body) {
        service.finish(id, body.get("resultDesc"));
        return ApiResult.ok();
    }

    @PutMapping("/close/{id}")
    @PreAuthorize("hasAuthority('repair:finish')")
    public ApiResult<Void> close(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        service.close(id, body == null ? null : body.get("remark"));
        return ApiResult.ok();
    }

    @PutMapping("/rate/{id}")
    public ApiResult<Void> rate(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        service.rate(id, body.get("rating"));
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('repair:delete')")
    public ApiResult<Void> delete(@PathVariable Long id) { service.remove(id); return ApiResult.ok(); }

    /* ---------- 故障照片 ---------- */

    /** 上传故障照片，返回文件ID */
    @PostMapping("/image")
    @PreAuthorize("hasAuthority('repair:create')")
    public ApiResult<FileObject> uploadImage(@RequestParam("file") MultipartFile file) {
        return ApiResult.ok(fileService.upload(file));
    }

    /** 故障照片预览（登录即可，供 <img> 直接引用） */
    @GetMapping("/image/{id}")
    public ResponseEntity<ByteArrayResource> previewImage(@PathVariable Long id) {
        FileObject f = fileService.get(id);
        byte[] data = fileService.download(id);
        MediaType type;
        try { type = MediaType.parseMediaType(f.getContentType()); }
        catch (Exception e) { type = MediaType.APPLICATION_OCTET_STREAM; }
        return ResponseEntity.ok().contentType(type)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .body(new ByteArrayResource(data));
    }

    private LocalDateTime parseTime(Object value) {
        if (value == null || value.toString().trim().isEmpty()) return null;
        String s = value.toString().trim().replace('T', ' ');
        if (s.length() == 16) s = s + ":00";
        try {
            return LocalDateTime.parse(s.replace(' ', 'T'));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}