package com.shengnan.platform.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 审计字段自动填充：创建人/时间、更新人/时间、逻辑删除默认值 */
@Component
public class AuditMetaHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        String user = currentUser();
        this.strictInsertFill(metaObject, "createdBy", String.class, user);
        this.strictInsertFill(metaObject, "updatedBy", String.class, user);
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedBy", String.class, currentUser());
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            Object p = auth.getPrincipal();
            if (p instanceof org.springframework.security.core.userdetails.UserDetails) {
                return ((org.springframework.security.core.userdetails.UserDetails) p).getUsername();
            }
            return String.valueOf(p);
        }
        return "system";
    }
}
