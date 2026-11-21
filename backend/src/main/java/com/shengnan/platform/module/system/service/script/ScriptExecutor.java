package com.shengnan.platform.module.system.service.script;

import org.codehaus.janino.ClassBodyEvaluator;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/** 运行时编译用户 Java 代码（Janino ClassBodyEvaluator），注入 Spring 容器 */
@Component
public class ScriptExecutor {
    private final ApplicationContext applicationContext;
    private final AtomicLong counter = new AtomicLong();

    private static final String[] IMPORTS = {
        "java.util.*", "java.io.*", "java.time.*", "java.math.*", "java.text.*",
        "com.shengnan.platform.module.system.service.script.*",
        "com.shengnan.platform.module.system.entity.*",
        "com.shengnan.platform.module.system.service.*"
    };

    public ScriptExecutor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /** 编译并检查语法（保存任务时调用，尽早发现错误） */
    public void validate(String code) {
        compile(code);
    }

    /** 编译并实例化 */
    private ScriptTask compile(String code) {
        if (code == null || code.trim().isEmpty()) throw new IllegalArgumentException("Java 代码不能为空");
        try {
            ClassBodyEvaluator cbe = new ClassBodyEvaluator();
            cbe.setImplementedInterfaces(new Class[]{ScriptTask.class});
            cbe.setDefaultImports(IMPORTS);
            cbe.setClassName("DynamicTask" + counter.incrementAndGet());
            cbe.cook(code);
            Class<?> clazz = cbe.getClazz();
            return (ScriptTask) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Java 代码编译失败: " + e.getMessage(), e);
        }
    }

    /** 执行代码，返回结果 + 日志 */
    public ScriptResult execute(String code) {
        ScriptTask task = compile(code);
        ScriptContext ctx = new ScriptContext(applicationContext);
        try {
            Object result = task.execute(ctx);
            return new ScriptResult(result, ctx.getLogs());
        } catch (Exception e) {
            throw new IllegalStateException("脚本执行失败: " + e.getMessage(), e);
        }
    }

    public static class ScriptResult {
        public final Object result;
        public final java.util.List<String> logs;
        public ScriptResult(Object result, java.util.List<String> logs) {
            this.result = result;
            this.logs = logs;
        }
    }
}
