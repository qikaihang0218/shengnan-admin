package com.shengnan.platform.module.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sp_token_blocklist")
public class TokenBlocklist implements Serializable {
    private Long id;
    private String tokenDigest;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
