package com.shengnan.platform.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 把 SQL 脚本按分号拆成独立语句。
 * 正确跳过：单引号字符串、双引号字符串、反引号标识符、-- 行注释、# 行注释、块注释。
 */
final class SqlStatementSplitter {

    private SqlStatementSplitter() {}

    static List<String> split(String script) {
        List<String> out = new ArrayList<>();
        if (script == null || script.isEmpty()) return out;

        StringBuilder current = new StringBuilder();
        int n = script.length();
        char quote = 0;

        for (int i = 0; i < n; i++) {
            char c = script.charAt(i);

            if (quote != 0) {
                current.append(c);
                if (c == '\\' && quote != '`' && i + 1 < n) {
                    current.append(script.charAt(++i));
                } else if (c == quote) {
                    if (i + 1 < n && script.charAt(i + 1) == quote) {
                        current.append(script.charAt(++i));
                    } else {
                        quote = 0;
                    }
                }
                continue;
            }

            if (c == '\'' || c == '"' || c == '`') {
                quote = c;
                current.append(c);
                continue;
            }

            if (c == '-' && i + 1 < n && script.charAt(i + 1) == '-') {
                while (i < n && script.charAt(i) != '\n') i++;
                current.append('\n');
                continue;
            }
            if (c == '#') {
                while (i < n && script.charAt(i) != '\n') i++;
                current.append('\n');
                continue;
            }
            if (c == '/' && i + 1 < n && script.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(script.charAt(i) == '*' && script.charAt(i + 1) == '/')) i++;
                i++;
                current.append(' ');
                continue;
            }

            if (c == ';') {
                addIfNotBlank(out, current.toString());
                current.setLength(0);
                continue;
            }

            current.append(c);
        }
        addIfNotBlank(out, current.toString());
        return out;
    }

    private static void addIfNotBlank(List<String> out, String sql) {
        String trimmed = sql.trim();
        if (trimmed.isEmpty()) return;
        if (trimmed.replaceAll("\\s+", "").isEmpty()) return;
        out.add(trimmed);
    }
}