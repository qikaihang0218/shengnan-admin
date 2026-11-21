package com.shengnan.platform.module.auth.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String captchaId;
    private String captchaCode;
    private Boolean bypassCaptcha;
}
