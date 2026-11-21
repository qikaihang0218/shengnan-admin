package com.shengnan.platform.module.system.service.script;

/** 动态 Java 代码需实现的接口：用户只写 execute 方法体 */
public interface ScriptTask {
    Object execute(ScriptContext ctx) throws Exception;
}
