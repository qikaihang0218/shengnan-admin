package com.shengnan.platform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 启动时自动执行外置自定义 SQL。
 * 脚本目录默认 ./sql（相对启动目录），文件名按字典序执行，如 V1__xxx.sql、V2__yyy.sql。
 * 已执行过的脚本记录在 sp_sql_migration，重复启动不会重复执行；内容被改动则按配置决定是否中断启动。
 */
@Slf4j
@Component
public class SqlMigrationRunner implements ApplicationRunner {

    @Value("${platform.sql.enabled:true}")
    private boolean enabled;

    @Value("${platform.sql.script-dir:./sql,../sql}")
    private String scriptDir;

    @Value("${platform.sql.classpath-dir:sql/migration}")
    private String classpathDir;

    @Value("${platform.sql.fail-on-checksum-mismatch:true}")
    private boolean failOnChecksumMismatch;

    @Value("${platform.sql.fail-fast:true}")
    private boolean failFast;

    private final JdbcTemplate jdbc;

    public SqlMigrationRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("[SQL] 自定义 SQL 自动执行已关闭（platform.sql.enabled=false）");
            return;
        }
        try {
            ensureHistoryTable();
            List<SqlScript> scripts = loadScripts();
            if (scripts.isEmpty()) {
                log.info("[SQL] 未发现自定义 SQL 脚本，外部目录={}，内置目录=classpath:{}", absDir(), classpathDir);
                return;
            }
            int executed = 0, skipped = 0;
            for (SqlScript script : scripts) {
                if (apply(script)) executed++; else skipped++;
            }
            log.info("[SQL] 自定义 SQL 执行完成：执行 {} 个，跳过 {} 个", executed, skipped);
        } catch (Exception e) {
            if (failFast) {
                throw new IllegalStateException("自定义 SQL 执行失败：" + e.getMessage(), e);
            }
            log.error("[SQL] 自定义 SQL 执行失败（已忽略，fail-fast=false）：{}", e.getMessage(), e);
        }
    }

    private void ensureHistoryTable() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS sp_sql_migration ("
                + " id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + " script_name VARCHAR(255) NOT NULL COMMENT '脚本文件名',"
                + " checksum VARCHAR(64) NOT NULL COMMENT '内容 SHA-256',"
                + " executed_at DATETIME DEFAULT NULL COMMENT '执行时间',"
                + " PRIMARY KEY (id),"
                + " UNIQUE KEY uk_sql_migration_name (script_name)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义 SQL 执行记录'");
    }

    private List<SqlScript> loadScripts() throws IOException {
        List<SqlScript> list = new ArrayList<>();

        Path dir = Paths.get(scriptDir).toAbsolutePath().normalize();
        if (Files.isDirectory(dir)) {
            try (Stream<Path> stream = Files.walk(dir, 1)) {
                for (Path p : stream.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".sql"))
                        .collect(Collectors.toList())) {
                    list.add(new SqlScript(p.getFileName().toString(),
                            new String(Files.readAllBytes(p), StandardCharsets.UTF_8)));
                }
            }
        } else {
            log.warn("[SQL] 自定义 SQL 目录不存在，已跳过：{}", dir);
        }

        if (list.isEmpty()) {
            log.warn("[SQL] 自定义 SQL 目录均不存在或为空：{}", absDir());
        }

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        String pattern = "classpath*:" + classpathDir.replaceAll("/+$", "") + "/*.sql";
        for (Resource r : resolver.getResources(pattern)) {
            String name = r.getFilename();
            if (name == null) continue;
            if (list.stream().anyMatch(s -> s.name.equals(name))) continue;
            list.add(new SqlScript(name, readFully(r)));
        }

        list.sort(Comparator.comparing(s -> s.name));
        return list;
    }

    private boolean apply(SqlScript script) {
        String checksum = sha256(script.content);
        List<java.util.Map<String, Object>> rows = jdbc.queryForList(
                "SELECT checksum FROM sp_sql_migration WHERE script_name = ?", script.name);

        if (!rows.isEmpty()) {
            String done = String.valueOf(rows.get(0).get("checksum"));
            if (done.equals(checksum)) {
                log.debug("[SQL] 已执行过，跳过：{}", script.name);
                return false;
            }
            String msg = "脚本内容已变更但此前已执行过：" + script.name
                    + "（如需重新执行，请另建新版本脚本，或删除 sp_sql_migration 中对应记录）";
            if (failOnChecksumMismatch) throw new IllegalStateException(msg);
            log.warn("[SQL] {}", msg);
            return false;
        }

        List<String> statements = SqlStatementSplitter.split(script.content);
        if (statements.isEmpty()) {
            log.info("[SQL] 脚本无有效语句，跳过：{}", script.name);
            return false;
        }
        log.info("[SQL] 开始执行 {}，共 {} 条语句", script.name, statements.size());
        for (int i = 0; i < statements.size(); i++) {
            String sql = statements.get(i);
            try {
                jdbc.execute(sql);
            } catch (RuntimeException e) {
                throw new IllegalStateException("脚本 " + script.name + " 第 " + (i + 1)
                        + " 条语句执行失败：" + e.getMessage() + "\nSQL: " + brief(sql), e);
            }
        }
        jdbc.update("INSERT INTO sp_sql_migration (script_name, checksum, executed_at) VALUES (?, ?, ?)",
                script.name, checksum, LocalDateTime.now());
        log.info("[SQL] 执行成功：{}", script.name);
        return true;
    }

    private static String readFully(Resource resource) throws IOException {
        try (java.io.InputStream in = resource.getInputStream()) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private String absDir() {
        return Stream.of(scriptDir.split("[,;]"))
                .map(s -> Paths.get(s.trim()).toAbsolutePath().normalize().toString())
                .collect(Collectors.joining(" | "));
    }

    private static String brief(String sql) {
        String s = sql.replaceAll("\\s+", " ").trim();
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }

    private static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("计算脚本校验值失败", e);
        }
    }

    private static final class SqlScript {
        final String name;
        final String content;

        SqlScript(String name, String content) {
            this.name = name;
            this.content = content;
        }
    }
}