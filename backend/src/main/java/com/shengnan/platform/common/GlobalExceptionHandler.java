package com.shengnan.platform.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 全局异常处理 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusiness(BusinessException e) {
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst().orElse("参数校验失败");
        return ApiResult.fail(400, msg);
    }

    @ExceptionHandler(BindException.class)
    public ApiResult<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst().orElse("参数绑定失败");
        return ApiResult.fail(400, msg);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ApiResult<Void> handleAccessDenied(AccessDeniedException e) {
        // 未认证（匿名）请求返回 401，让前端跳登录；已认证但无权限返回 403
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean anonymous = auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal());
        if (anonymous) return ApiResult.fail(401, "登录已失效");
        return ApiResult.fail(403, "无权限访问该资源");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return ApiResult.fail(500, "系统异常: " + e.getMessage());
    }
}
