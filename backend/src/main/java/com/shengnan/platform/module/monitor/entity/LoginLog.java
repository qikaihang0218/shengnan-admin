package com.shengnan.platform.module.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sp_login_log")
public class LoginLog implements Serializable {
    private Long id;
    private String username;
    private String ipAddr;
    private String userAgent;
    private String osName;
    private Boolean isSuccess;
    private String message;
    private LocalDateTime loginAt;
}
