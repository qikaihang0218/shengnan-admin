package com.shengnan.platform.module.monitor.controller;

import com.shengnan.platform.common.ApiResult;
import com.shengnan.platform.module.monitor.entity.FileObject;
import com.shengnan.platform.module.monitor.service.FileService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/file")
public class FileController {
    private final FileService service;

    public FileController(FileService service) { this.service = service; }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('file:upload')")
    public ApiResult<FileObject> upload(@RequestParam("file") MultipartFile file) {
        return ApiResult.ok(service.upload(file));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('file:list')")
    public ApiResult<List<FileObject>> list() { return ApiResult.ok(service.list()); }

    @GetMapping("/download/{id}")
    @PreAuthorize("hasAuthority('file:download')")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long id) throws java.io.UnsupportedEncodingException {
        FileObject f = service.get(id);
        byte[] data = service.download(id);
        String encoded = URLEncoder.encode(f.getOriginalName(), "UTF-8").replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentLength(data.length)
                .body(new ByteArrayResource(data));
    }

    @GetMapping("/preview/{id}")
    public ResponseEntity<ByteArrayResource> preview(@PathVariable Long id) {
        FileObject f = service.get(id);
        byte[] data = service.download(id);
        MediaType type;
        try { type = MediaType.parseMediaType(f.getContentType()); }
        catch (Exception e) { type = MediaType.APPLICATION_OCTET_STREAM; }
        return ResponseEntity.ok().contentType(type).body(new ByteArrayResource(data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('file:delete')")
    public ApiResult<Void> delete(@PathVariable Long id) { service.delete(id); return ApiResult.ok(); }
}
