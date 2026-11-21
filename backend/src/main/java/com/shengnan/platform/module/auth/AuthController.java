package com.shengnan.platform.module.auth;

import com.shengnan.platform.common.ApiResult;
import com.shengnan.platform.common.BusinessException;
import com.shengnan.platform.module.auth.dto.LoginRequest;
import com.shengnan.platform.module.monitor.entity.LoginLog;
import com.shengnan.platform.module.monitor.mapper.LoginLogMapper;
import com.shengnan.platform.security.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final TokenBlocklistService blocklistService;
    private final CaptchaService captchaService;
    private final LoginLogMapper loginLogMapper;
    private final AuthCookie authCookie;

    public AuthController(UserDetailsServiceImpl userDetailsService, PasswordEncoder passwordEncoder,
                          JwtTokenProvider tokenProvider, TokenBlocklistService blocklistService,
                          CaptchaService captchaService, LoginLogMapper loginLogMapper, AuthCookie authCookie) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.blocklistService = blocklistService;
        this.captchaService = captchaService;
        this.loginLogMapper = loginLogMapper;
        this.authCookie = authCookie;
    }

    @GetMapping("/health")
    public ApiResult<Map<String, Object>> health() {
        Map<String, Object> m = new HashMap<>();
        m.put("status", "UP");
        m.put("platform", "胜楠管理平台");
        return ApiResult.ok(m);
    }

    @GetMapping("/captcha")
    public ApiResult<Map<String, Object>> captcha() {
        return ApiResult.ok(captchaService.generate());
    }

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@RequestBody LoginRequest req, HttpServletRequest http,
                                                 HttpServletResponse response) {
        if (req.getBypassCaptcha() == null || !req.getBypassCaptcha()) {
            captchaService.verify(req.getCaptchaId(), req.getCaptchaCode());
        }
        LoginPrincipal principal;
        try {
            principal = (LoginPrincipal) userDetailsService.loadUserByUsername(req.getUsername());
        } catch (BusinessException e) {
            recordLogin(req.getUsername(), http, false, e.getMessage());
            throw e;
        }
        if (!passwordEncoder.matches(req.getPassword(), principal.getPassword())) {
            recordLogin(req.getUsername(), http, false, "密码错误");
            throw new BusinessException("用户名或密码错误");
        }
        String token = tokenProvider.createToken(principal.getAccountId(), principal.getUsername());
        // 令牌只通过 HttpOnly Cookie 下发，不出现在响应体，JS 无法读取
        authCookie.write(response, token);
        recordLogin(req.getUsername(), http, true, "登录成功");

        Map<String, Object> data = new HashMap<>();
        data.put("username", principal.getUsername());
        data.put("displayName", principal.getUsername());
        data.put("roles", principal.getRoles());
        data.put("permissions", principal.getPermissions());
        data.put("dataScope", principal.getDataScope());
        return ApiResult.ok(data);
    }

    @GetMapping("/info")
    public ApiResult<Map<String, Object>> info() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginPrincipal)) {
            return ApiResult.fail(HttpStatus.UNAUTHORIZED.value(), "未登录");
        }
        LoginPrincipal p = (LoginPrincipal) auth.getPrincipal();
        Map<String, Object> data = new HashMap<>();
        data.put("username", p.getUsername());
        data.put("roles", p.getRoles());
        data.put("permissions", p.getPermissions());
        data.put("dataScope", p.getDataScope());
        return ApiResult.ok(data);
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpServletRequest http, HttpServletResponse response) {
        // Cookie 优先，Authorization 头兜底，两种客户端都能正确拉黑令牌
        String token = authCookie.read(http);
        if (token == null || token.isEmpty()) {
            String header = http.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) token = header.substring(7);
        }
        if (token != null && !token.isEmpty()) {
            try { blocklistService.block(token, tokenProvider.expiresInMillis(token)); }
            catch (Exception ignored) { /* 令牌异常时仍清除会话 */ }
        }
        authCookie.clear(response);
        SecurityContextHolder.clearContext();
        return ApiResult.ok();
    }

    private void recordLogin(String username, HttpServletRequest http, boolean success, String message) {
        try {
            LoginLog log = new LoginLog();
            log.setUsername(username);
            log.setIpAddr(http.getRemoteAddr());
            log.setUserAgent(http.getHeader("User-Agent"));
            log.setOsName(extractOs(http.getHeader("User-Agent")));
            log.setIsSuccess(success);
            log.setMessage(message);
            log.setLoginAt(LocalDateTime.now());
            loginLogMapper.insert(log);
        } catch (Exception ignored) {
            // 登录记录失败不影响登录本身
        }
    }

    private String extractOs(String ua) {
        if (ua == null) return "未知";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        return "未知";
    }
}
