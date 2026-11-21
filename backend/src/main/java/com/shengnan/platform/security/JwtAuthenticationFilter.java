package com.shengnan.platform.security;

import com.shengnan.platform.common.ApiResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * JWT 过滤器：公开路径精确放行，其余路径解析令牌 + 校验黑名单。
 * 令牌来源优先级：HttpOnly Cookie（浏览器） > Authorization 头（API 客户端兼容）。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final TokenBlocklistService blocklistService;
    private final UserDetailsServiceImpl userDetailsService;
    private final AuthCookie authCookie;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> PUBLIC_PATHS = new HashSet<>(Arrays.asList(
            "/api/auth/login", "/api/auth/captcha", "/api/auth/health"
    ));

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, TokenBlocklistService blocklistService,
                                   UserDetailsServiceImpl userDetailsService, AuthCookie authCookie) {
        this.tokenProvider = tokenProvider;
        this.blocklistService = blocklistService;
        this.userDetailsService = userDetailsService;
        this.authCookie = authCookie;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if (PUBLIC_PATHS.contains(path)) {
            chain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);
        if (token != null && !token.isEmpty()) {
            try {
                if (blocklistService.isBlocked(token)) {
                    writeError(response, 401, "登录已失效，请重新登录");
                    return;
                }
                String username = tokenProvider.username(token);
                LoginPrincipal principal = (LoginPrincipal) userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                writeError(response, 401, "令牌无效或已过期");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    /** Cookie 优先，Authorization 头兜底 */
    private String resolveToken(HttpServletRequest request) {
        String fromCookie = authCookie.read(request);
        if (fromCookie != null && !fromCookie.isEmpty()) return fromCookie;
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) return header.substring(7);
        return null;
    }

    private void writeError(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResult.fail(code, message)));
    }
}
