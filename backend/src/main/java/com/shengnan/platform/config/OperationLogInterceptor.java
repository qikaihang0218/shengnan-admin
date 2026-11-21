package com.shengnan.platform.config;

import com.shengnan.platform.module.monitor.entity.OperationLog;
import com.shengnan.platform.module.monitor.mapper.OperationLogMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

/**
 * 记录写操作（POST/PUT/DELETE）到操作日志表。
 * 只记录请求元信息，不读取请求体，避免破坏后续参数绑定。
 */
@Component
public class OperationLogInterceptor implements HandlerInterceptor {

    private static final String ATTR_START = "__opLogStart";

    private final OperationLogMapper mapper;

    public OperationLogInterceptor(OperationLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(ATTR_START, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String method = request.getMethod();
        if (!("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method))) return;

        String uri = request.getRequestURI();
        if (uri.startsWith("/api/auth/") || uri.startsWith("/api/operation-log")) return;

        try {
            Object start = request.getAttribute(ATTR_START);
            long cost = start instanceof Long ? System.currentTimeMillis() - (Long) start : 0L;

            OperationLog log = new OperationLog();
            log.setModuleName(moduleOf(uri));
            log.setActionDesc(actionOf(method) + " " + uri);
            log.setMethodName(handler.getClass().getSimpleName());
            log.setRequestUri(uri);
            log.setIpAddr(clientIp(request));
            log.setOperator(currentUser());
            log.setCostMs((int) cost);
            log.setIsSuccess(response.getStatus() < 400 && ex == null);
            log.setErrorMsg(ex != null ? truncate(ex.getMessage(), 500) : null);
            log.setOperatedAt(LocalDateTime.now());
            mapper.insert(log);
        } catch (Exception ignored) {
            // 日志记录失败不影响业务请求
        }
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() ? auth.getName() : "anonymous";
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }

    private String moduleOf(String uri) {
        String[] parts = uri.split("/");
        return parts.length > 2 ? parts[2] : "unknown";
    }

    private String actionOf(String method) {
        switch (method) {
            case "POST": return "新增";
            case "PUT": return "修改";
            case "DELETE": return "删除";
            default: return method;
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}