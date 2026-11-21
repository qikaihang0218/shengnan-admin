package com.shengnan.platform.module.system.service.script;

import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

/** 脚本执行上下文：暴露 Spring 容器 + 日志收集 */
public class ScriptContext {
    private final ApplicationContext applicationContext;
    private final List<String> logs = new ArrayList<>();

    public ScriptContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public <T> T getBean(Class<T> type) { return applicationContext.getBean(type); }

    public Object getBean(String name) { return applicationContext.getBean(name); }

    public void log(Object message) { logs.add(String.valueOf(message)); }

    public List<String> getLogs() { return logs; }
}
