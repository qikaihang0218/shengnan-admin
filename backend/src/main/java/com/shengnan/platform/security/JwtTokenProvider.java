package com.shengnan.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/** JWT 签发与解析 */
@Component
public class JwtTokenProvider {

    @Value("${platform.jwt.secret}")
    private String secret;

    @Value("${platform.jwt.expiration-hours:24}")
    private long expirationHours;

    public String createToken(Long accountId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("accountId", accountId);
        claims.put("username", username);
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationHours * 3600_000L);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }

    public String username(String token) {
        return parse(token).getSubject();
    }

    public Long accountId(String token) {
        Object v = parse(token).get("accountId");
        return v == null ? null : ((Number) v).longValue();
    }

    public long expiresInMillis(String token) {
        return parse(token).getExpiration().getTime() - System.currentTimeMillis();
    }
}
