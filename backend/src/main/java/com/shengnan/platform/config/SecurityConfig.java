package com.shengnan.platform.config;

import com.shengnan.platform.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .cors().configurationSource(corsSource())
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                .antMatchers("/api/auth/login", "/api/auth/captcha", "/api/auth/health").permitAll()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .exceptionHandling()
                .authenticationEntryPoint(this::unauthorized)
                .accessDeniedHandler((req, resp, ex) -> writeJson(resp, 403, "无访问权限"))
            .and()
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void unauthorized(javax.servlet.http.HttpServletRequest req,
                              javax.servlet.http.HttpServletResponse resp,
                              org.springframework.security.core.AuthenticationException ex) throws java.io.IOException {
        writeJson(resp, 401, "未登录");
    }

    private void writeJson(javax.servlet.http.HttpServletResponse resp, int code, String message) throws java.io.IOException {
        resp.setStatus(code);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}");
    }

    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(Arrays.asList("*"));
        c.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(Arrays.asList("*"));
        c.setAllowCredentials(true);
        c.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
