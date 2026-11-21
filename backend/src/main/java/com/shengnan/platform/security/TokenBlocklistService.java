package com.shengnan.platform.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shengnan.platform.module.monitor.entity.TokenBlocklist;
import com.shengnan.platform.module.monitor.mapper.TokenBlocklistMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

/** 令牌黑名单：登出后 token 失效 */
@Service
public class TokenBlocklistService {
    private final TokenBlocklistMapper mapper;

    public TokenBlocklistService(TokenBlocklistMapper mapper) {
        this.mapper = mapper;
    }

    public void block(String token, long expiresInMillis) {
        String digest = digest(token);
        TokenBlocklist row = new TokenBlocklist();
        row.setTokenDigest(digest);
        row.setExpiresAt(LocalDateTime.now().plusNanos(expiresInMillis * 1_000_000L));
        row.setCreatedAt(LocalDateTime.now());
        mapper.insert(row);
    }

    public boolean isBlocked(String token) {
        String digest = digest(token);
        Long count = mapper.selectCount(Wrappers.<TokenBlocklist>lambdaQuery()
                .eq(TokenBlocklist::getTokenDigest, digest)
                .gt(TokenBlocklist::getExpiresAt, LocalDateTime.now()));
        return count != null && count > 0;
    }

    public void cleanExpired() {
        mapper.delete(Wrappers.<TokenBlocklist>lambdaQuery()
                .le(TokenBlocklist::getExpiresAt, LocalDateTime.now()));
    }

    private String digest(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("摘要计算失败", e);
        }
    }
}
