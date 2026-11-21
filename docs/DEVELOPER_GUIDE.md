# 胜楠管理平台 · 开发者文档

> 面向二次开发者。技术栈：Java 8 + Spring Boot 2.7 + MyBatis-Plus + JWT（后端）；Vue 3 + Vite 5 + Element Plus + Pinia（前端）；MySQL 5.7+。

---

## 1. 环境要求

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 8 | 编译目标 Java 1.8 |
| Maven | 3.6+ | 或用 IDEA 内置 Maven |
| Node.js | 16+ | 前端构建 |
| MySQL | 5.7+ | utf8mb4 字符集 |

## 2. 快速启动

### 2.1 初始化数据库
```bash
mysql -uroot -p < backend/src/main/resources/sql/init.sql
```
脚本会创建 `shengnan_platform` 库、16 张表、初始化 admin 账号与权限数据。**可重复执行**（会先 DROP 表，注意会清空数据）。

### 2.2 配置
编辑 `backend/src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/shengnan_platform?...
    username: root
    password: admin        # 改成你的密码
platform:
  jwt:
    secret: ...            # 生产环境务必修改
  upload:
    dir: ./uploads         # 文件存储目录
```

### 2.3 启动后端
```bash
cd backend
mvn clean package -DskipTests
java -jar target/shengnan-admin-1.0.0.jar
```
默认端口 `8081`，健康检查：`GET /api/auth/health`。

### 2.4 启动前端
```bash
cd frontend
npm install
npm run dev
```
默认端口 `5174`，Vite 已将 `/api` 代理到 `http://localhost:8081`。

### 2.5 IDEA 直接运行
- 后端：导入 `backend` 为 Maven 项目，运行 `com.shengnan.platform.PlatformApplication`。
- 前端：在 `frontend` 目录执行 `npm run dev`。

---

## 3. 项目结构

```
shengnan-admin/
├── backend/
│   └── src/main/
│       ├── java/com/shengnan/platform/
│       │   ├── PlatformApplication.java    # 启动类
│       │   ├── common/                      # 返回体/异常/分页/审计/CRUD基类
│       │   ├── config/                      # Security/MybatisPlus 配置
│       │   ├── security/                    # JWT/登录主体/黑名单/UserDetails
│       │   └── module/
│       │       ├── auth/                    # 登录/验证码
│       │       ├── system/                  # 账号/角色/权限/部门/岗位/字典/参数
│       │       └── monitor/                 # 日志/文件/定时任务
│       └── resources/
│           ├── application.yml
│           └── sql/init.sql
└── frontend/
    └── src/
        ├── api/            # （请求统一走 utils/request.js）
        ├── router/         # 动态路由
        ├── store/          # Pinia 用户状态
        ├── layout/         # 主布局
        ├── views/          # 页面
        └── utils/request.js
```

## 4. 数据模型约定

| 约定 | 说明 |
|---|---|
| 表前缀 | `sp_`（shengnan platform） |
| 主键 | 统一 `id`，`AUTO_INCREMENT` |
| 审计字段 | `created_by / created_at / updated_by / updated_at`，由 `AuditMetaHandler` 自动填充 |
| 逻辑删除 | `is_deleted`（0 存在 / 1 删除），MyBatis-Plus `@TableLogic` |
| 状态 | `is_enabled`（tinyint 1 启用 / 0 停用） |
| 权限树 | `sp_permission`，`resource_type` 取 `catalog/menu/action` |
| 权限标识 | `资源:动作` 格式，如 `account:create`、`role:grant` |
| 数据范围 | `sp_role.data_scope`：`all/dept/dept_tree/self` |

## 5. 认证与鉴权流程

```
登录 → 校验验证码 → 校验密码(BCrypt) → 签发 JWT
  ↓
Set-Cookie: sn_token=<jwt>; HttpOnly; SameSite=Lax; Path=/
（响应体不含 token，JS 无法读取）
  ↓
后续请求浏览器自动携带 Cookie（axios withCredentials: true）
JwtAuthenticationFilter 按 Cookie 优先 / Authorization 头兜底解析 token
  → 校验黑名单 → 加载 LoginPrincipal → 写入 SecurityContext
  ↓
@PreAuthorize("hasAuthority('xxx')") 做按钮级校验
```

- **令牌载体**：`security/AuthCookie.java` 统一封装 Cookie 写入/清除/读取。`HttpOnly` 阻断 JS 读取（防 XSS 窃取令牌），`SameSite=Lax` 拦截跨站写请求（CSRF 基础防护）。配置项 `platform.cookie.secure`（本地 http 为 false，生产 HTTPS 必须改 true）与 `platform.cookie.same-site`。
- **前端登录态**：`store/user.js` 不再接触令牌，仅维护 `checked` 状态；刷新后由路由守卫调用 `/auth/info` 依托 Cookie 自动恢复会话。文件上传/下载/预览用 `credentials: 'include'` 携带 Cookie，不再拼 `Authorization` 头。
- **API 兼容**：服务端仍接受 `Authorization: Bearer <token>` 作为兜底，方便脚本/非浏览器客户端调用。
- **公开路径**（无需 token）：`/api/auth/login`、`/api/auth/captcha`、`/api/auth/health`。
- **登出**：清除 Cookie，并将令牌写入 `sp_token_blocklist`，未过期前不可再用。
- **权限加载**：`UserDetailsServiceImpl` 从角色→权限关联加载；超管自动获得全部权限。

## 6. 如何新增一个业务模块

以已实现的「通知公告」模块为例：

1. **建表**：`sp_notice`（通知，含审计字段 + `status/publish_time/is_top`）+ `sp_notice_read`（已读记录，`uk(notice_id, account_id)`）。全新安装见 `init.sql`，旧库升级见 `notice.sql`。
2. **实体**：`module/system/entity/Notice.java` 继承 `BaseEntity`，`@TableName("sp_notice")`；已读标记/类型名用 `@TableField(exist = false)` 非表字段承载。
3. **Mapper**：`NoticeMapper` / `NoticeReadMapper` 继承 `BaseMapper`。
4. **Service**：`NoticeService`（未继承 CrudService，因含跨表已读逻辑）。管理端 CRUD/发布，用户端 `published()` 按 `is_top/publish_time` 排序并回填 `read` 标记，`markRead`/`markAllRead` 幂等写入已读表。当前账号 ID 从 `SecurityContextHolder` 的 `LoginPrincipal.accountId` 获取。
5. **Controller**：`@RequestMapping("/api/notice")`。管理端接口加 `@PreAuthorize("hasAuthority('notice:list/create/update/delete/publish')")`；**用户端接口（`published`/`unread-count`/`read`/`read-all`）不加权限注解，仅需登录**，保证全员可读。
6. **菜单**：`sp_permission` 插入 1108 菜单 + 110801~110805 动作，`notice:list` 同时授予访客角色以显示入口。
7. **前端**：`views/system/notice/index.vue` 管理页 + `router/index.js` 注册组件；用户端入口在 `layout/index.vue`（铃铛抽屉 + 未读角标）与 `views/dashboard/index.vue`（公告栏）。

> 权限语义约定：管理接口 403（已认证无权限），未认证请求由全局异常处理器返回 401（body code），与前端 `request.js` 的登录跳转逻辑对应。

## 7. 定时任务扩展

内置 bean 方法定义在 `module/monitor/service/SystemTasks.java`，调用目标格式 `systemTasks.方法名()`。新增方法后编译即可在页面下拉选择。

- **Shell 类型**：`ProcessBuilder` 执行，Windows 用 `cmd /c`，Linux 用 `sh -c`；超时由 `platform.job.script-timeout-seconds` 控制（默认 300s）。
- **Java 代码类型**：基于 Janino `ClassBodyEvaluator` 运行时编译。用户代码需实现 `ScriptTask.execute(ScriptContext ctx)`，可用 `ctx.getBean()` 调用任意 Spring Bean。保存时即编译校验，语法错误无法入库。
- **安全**：Shell/Java 类型受 `job:script` 权限保护，只授予可信用户。

## 8. 安全建议

- 生产环境务必修改 `platform.jwt.secret`，并把 `platform.cookie.secure` 改为 `true`（需 HTTPS）。
- 具有高危能力（`job:script`）的角色只分配给可信管理员。
- 默认账号 `admin/admin123` 首次登录后立即改密。
- 文件上传目录不要放在 Web 静态资源目录下。
- **富文本安全**：通知内容采用「先整体 HTML 转义 + 白名单标签生成」的渲染策略（`frontend/src/utils/safeMarkdown.js` + `components/SafeMarkdown.vue`）。新增任何用户可控内容的展示时，禁止直接 `v-html` 原始输入；如需富文本，必须走同一套转义/白名单流程。链接协议仅放行 http/https/mailto。认证令牌已放入 HttpOnly Cookie，即使未来出现 XSS，脚本也无法读取令牌，形成纵深防御。

## 9. 常见问题

**Q：编译报 `Map.of()` / `List.of()` 找不到？**
A：项目是 Java 8，不要用 Java 9+ 的集合工厂方法，改用 `new HashMap<>()`。

**Q：实体查询报 `Unknown column 'xxx'`？**
A：确认表中存在该字段。继承 `BaseEntity` 的实体会查询 `created_by/created_at/updated_by/updated_at/is_deleted`，表结构必须包含这些列。

**Q：MyBatis-Plus 分页不生效？**
A：确认 `MybatisPlusConfig` 中注册了 `PaginationInnerInterceptor`。

**Q：删除记录后还在？**
A：逻辑删除字段 `is_deleted` 必须调用 `deleteById`/`delete`（MP 自动转 UPDATE），不要用自定义 SQL 的 `UPDATE ... SET`。
