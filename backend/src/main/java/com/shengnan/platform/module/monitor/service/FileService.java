package com.shengnan.platform.module.monitor.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shengnan.platform.common.BusinessException;
import com.shengnan.platform.module.monitor.entity.FileObject;
import com.shengnan.platform.module.monitor.mapper.FileObjectMapper;
import com.shengnan.platform.security.LoginPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {
    private final FileObjectMapper mapper;

    @Value("${platform.upload.dir:./uploads}")
    private String uploadDir;

    public FileService(FileObjectMapper mapper) { this.mapper = mapper; }

    public FileObject upload(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("文件不能为空");
        String original = file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf(".")) : "";
        String objectKey = UUID.randomUUID().toString().replace("-", "") + ext;
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(objectKey).normalize();
            if (!target.startsWith(dir)) throw new BusinessException("非法文件路径");
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            FileObject row = new FileObject();
            row.setOriginalName(original);
            row.setObjectKey(objectKey);
            row.setStoragePath(target.toString());
            String ct = file.getContentType();
            row.setContentType(ct == null ? "application/octet-stream" : ct);
            row.setSizeBytes(file.getSize());
            row.setUploader(currentUser());
            row.setIsDeleted(0);
            row.setUploadedAt(LocalDateTime.now());
            mapper.insert(row);
            return row;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("上传失败: " + e.getMessage());
        }
    }

    public List<FileObject> list() {
        return mapper.selectList(Wrappers.<FileObject>lambdaQuery().orderByDesc(FileObject::getUploadedAt));
    }

    public FileObject get(Long id) {
        FileObject f = mapper.selectById(id);
        if (f == null) throw new BusinessException("文件不存在");
        return f;
    }

    public byte[] download(Long id) {
        FileObject f = get(id);
        try {
            return Files.readAllBytes(Paths.get(f.getStoragePath()));
        } catch (Exception e) {
            throw new BusinessException("文件读取失败（可能已被物理删除）");
        }
    }

    /** 逻辑删除 + 物理删除文件 */
    public void delete(Long id) {
        FileObject f = get(id);
        mapper.deleteById(id);
        try {
            Files.deleteIfExists(Paths.get(f.getStoragePath()));
        } catch (Exception ignored) {
            // 物理删除失败不影响逻辑删除
        }
    }

    private String currentUser() {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (p instanceof LoginPrincipal) return ((LoginPrincipal) p).getUsername();
        return "system";
    }
}
