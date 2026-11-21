package com.shengnan.platform.module.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sp_file")
public class FileObject implements Serializable {
    private Long id;
    private String originalName;
    private String objectKey;
    private String storagePath;
    private String contentType;
    private Long sizeBytes;
    private String uploader;
    @TableLogic
    private Integer isDeleted;
    private LocalDateTime uploadedAt;
}
