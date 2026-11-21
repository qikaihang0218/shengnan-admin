package com.shengnan.platform.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 认证令牌 Cookie 封装：HttpOnly + SameSite + 可选 Secure。
 * 令牌不进入响应体、不落入 JS 可读写存储，从根源上阻断 XSS 窃取。
 */
@Component
public class AuthCookie {

    public static final String NAME = "sn_token";

    @Value("${platform.cookie.secure:false}")
    private boolean secure;

    @Value("${platform.cookie.same-site:Lax}")
    private String sameSite;

    @Value("${platform.jwt.expiration-hours:24}")
    private long expirationHours;

    /** 登录成功写入令牌 Cookie */
    public void write(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(expirationHours * 3600L)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /** 登出清除令牌 Cookie */
    public void clear(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /** 从请求读取令牌 Cookie */
    public String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (NAME.equals(c.getName())) return c.getValue();
            }
        }
        return null;
    }
}
