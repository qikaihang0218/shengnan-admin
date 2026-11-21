package com.shengnan.platform.module.monitor.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shengnan.platform.common.BusinessException;
import com.shengnan.platform.module.monitor.entity.Job;
import com.shengnan.platform.module.monitor.entity.JobLog;
import com.shengnan.platform.module.monitor.mapper.JobLogMapper;
import com.shengnan.platform.module.system.service.script.ScriptExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.concurrent.*;

/** 定时任务执行器：bean 反射 / shell 命令 / java 代码，记录运行日志 */
@Component
public class JobExecutor {
    private final ApplicationContext applicationContext;
    private final JobLogMapper jobLogMapper;
    private final ScriptExecutor scriptExecutor;
    private final ExecutorService pool = Executors.newCachedThreadPool();

    @Value("${platform.job.script-timeout-seconds:300}")
    private long timeoutSeconds = 300L;

    private static final int MAX_SCRIPT = 100_000;

    public JobExecutor(ApplicationContext applicationContext, JobLogMapper jobLogMapper,
                       ScriptExecutor scriptExecutor) {
        this.applicationContext = applicationContext;
        this.jobLogMapper = jobLogMapper;
        this.scriptExecutor = scriptExecutor;
    }

    /** 保存任务前校验 */
    public void validate(Job job) {
        String kind = job.getJobKind() == null ? "bean" : job.getJobKind();
        if ("bean".equals(kind)) {
            String t = job.getInvokeTarget();
            if (t == null || !t.matches("^[A-Za-z0-9_]+\\.[A-Za-z0-9_]+\\(\\)$")) {
                throw new BusinessException("调用目标格式应为 bean.method()，如 systemTasks.cleanLogs()");
            }
        } else if ("shell".equals(kind)) {
            String s = job.getScriptBody();
            if (s == null || s.trim().isEmpty()) throw new BusinessException("Shell 脚本内容不能为空");
            if (s.length() > MAX_SCRIPT) throw new BusinessException("脚本内容过长");
        } else if ("java".equals(kind)) {
            String s = job.getScriptBody();
            if (s == null || s.trim().isEmpty()) throw new BusinessException("Java 代码不能为空");
            if (s.length() > MAX_SCRIPT) throw new BusinessException("代码内容过长");
            try { scriptExecutor.validate(s); }
            catch (Exception e) { throw new BusinessException("Java 代码校验失败: " + e.getMessage()); }
        } else {
            throw new BusinessException("未知的任务类型: " + kind);
        }
    }

    /** 执行任务并记录日志，返回消息 */
    public String run(Job job, String trigger) {
        String kind = job.getJobKind() == null ? "bean" : job.getJobKind();
        long start = System.currentTimeMillis();
        String message;
        boolean success;
        try {
            Object result;
            if ("shell".equals(kind)) result = runShell(job.getScriptBody());
            else if ("java".equals(kind)) result = runJava(job.getScriptBody());
            else result = invokeBean(job.getInvokeTarget());
            String r = result == null ? "执行成功" : String.valueOf(result);
            if (r.length() > 1800) r = r.substring(0, 1800) + "...";
            message = "[" + trigger + "] " + r;
            success = true;
        } catch (Exception e) {
            message = "[" + trigger + "] 执行失败: " + e.getMessage();
            if (message.length() > 1800) message = message.substring(0, 1800);
            success = false;
        }
        saveLog(job, kind, success, message, (int) (System.currentTimeMillis() - start));
        return message;
    }

    private Object invokeBean(String target) throws Exception {
        String body = target.substring(0, target.length() - 2);
        String[] parts = body.split("\\.");
        Object bean = applicationContext.getBean(parts[0]);
        Method method = bean.getClass().getMethod(parts[1]);
        return method.invoke(bean);
    }

    private Object runShell(String script) throws Exception {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        ProcessBuilder pb = windows
                ? new ProcessBuilder("cmd", "/c", script)
                : new ProcessBuilder("sh", "-c", script);
        Process process = pb.start();
        Charset charset = Charset.defaultCharset();
        Future<String> output = pool.submit(() -> readAll(process.getInputStream(), charset));
        Future<String> error = pool.submit(() -> readAll(process.getErrorStream(), charset));
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("脚本执行超时（" + timeoutSeconds + "秒）");
        }
        String out = output.get(5, TimeUnit.SECONDS);
        String err = error.get(5, TimeUnit.SECONDS);
        int code = process.exitValue();
        String combined = (out == null ? "" : out) + (err == null ? "" : err);
        if (code != 0) throw new IllegalStateException("退出码 " + code + "：" + combined.trim());
        return combined.trim().isEmpty() ? "脚本执行完成（退出码 0）" : combined.trim();
    }

    private Object runJava(String code) {
        ScriptExecutor.ScriptResult r = scriptExecutor.execute(code);
        StringBuilder sb = new StringBuilder();
        if (r.logs != null && !r.logs.isEmpty()) {
            sb.append(String.join("\n", r.logs));
        }
        if (r.result != null) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("返回: ").append(r.result);
        }
        return sb.length() == 0 ? "执行成功（无输出）" : sb.toString();
    }

    private String readAll(java.io.InputStream in, Charset charset) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, charset))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString();
    }

    private void saveLog(Job job, String kind, boolean success, String message, int costMs) {
        try {
            JobLog log = new JobLog();
            log.setJobId(job.getId());
            log.setJobName(job.getJobName());
            log.setJobKind(kind);
            String brief = "shell".equals(kind) || "java".equals(kind)
                    ? "[" + kind.toUpperCase() + "] " + brief(job.getScriptBody(), 260)
                    : job.getInvokeTarget();
            log.setTargetBrief(brief);
            log.setIsSuccess(success);
            log.setLogMessage(message);
            log.setCostMs(costMs);
            log.setFiredAt(LocalDateTime.now());
            jobLogMapper.insert(log);
        } catch (Exception ignored) {
            // 日志写入失败不影响任务执行
        }
    }

    private String brief(String s, int max) {
        if (s == null) return "";
        String one = s.replaceAll("\\s+", " ").trim();
        return one.length() > max ? one.substring(0, max) + "..." : one;
    }
}
