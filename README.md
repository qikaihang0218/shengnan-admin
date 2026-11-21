# 胜楠管理平台（shengnan-admin）

一个**独立实现**的企业级权限管理系统，前后端分离架构。包含用户认证、RBAC 权限、数据权限、文件管理、审计日志、可视化定时任务、全员通知与多主题切换。

> 本项目为独立设计实现，采用自有数据模型（`sp_` 前缀）、字段约定与权限模型，不依赖任何第三方后台框架源码。
>
> 仓库地址：https://github.com/qikaihang0218/shengnan-admin.git

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 8 · Spring Boot 2.7 · Spring Security · JWT · MyBatis-Plus 3.5 · Janino |
| 前端 | Vue 3 · Vite 5 · Element Plus · Pinia · Vue Router 4 · Axios |
| 数据库 | MySQL 5.7+（utf8mb4） |

## 功能特性

- ✅ **认证**：账号密码登录 + 图形验证码 + JWT（HttpOnly Cookie 承载）+ 令牌黑名单（登出即失效）
- ✅ **动态路由**：后端按角色权限生成菜单，刷新后从用户信息恢复
- ✅ **按钮级权限**：`@PreAuthorize` + 前端 `hasPerm()` 双重控制
- ✅ **数据权限**：全部 / 本部门 / 本部门及以下 / 仅本人
- ✅ **账号管理**：增删改查、启停、重置密码、分配角色与部门
- ✅ **角色管理**：权限树勾选分配、数据范围配置
- ✅ **权限资源**：目录/菜单/动作三级树形维护
- ✅ **部门 / 岗位 / 字典 / 系统参数**：完整维护界面
- ✅ **登录记录 / 操作日志**：审计追踪 + 清空
- ✅ **文件管理**：上传、在线预览、下载、删除
- ✅ **定时任务**：可视化调度，三种类型（Java 方法 / Shell 脚本 / Java 代码），Spring Task 动态注册
- ✅ **全员通知**：管理员发布通知/公告，全体账号通过顶部铃铛查看，未读角标 + 已读记录；内容支持安全的 Markdown 渲染（HTML/脚本自动过滤）
- ✅ **多主题**：5 套主题（4 浅色主色 + 1 深色模式），顶栏一键切换，本地持久化

## 快速启动

```bash
# 1. 初始化数据库（创建 shengnan_platform）
mysql -uroot -p < backend/src/main/resources/sql/init.sql

# 2. 启动后端（端口 8081）
cd backend && mvn clean package -DskipTests && java -jar target/shengnan-admin-1.0.0.jar

# 3. 启动前端（端口 5174）
cd frontend && npm install && npm run dev
```

浏览器打开 `http://localhost:5174`，默认账号 `admin` / `admin123`。

### 环境变量（可选，生产必配）

数据库账号密码与 JWT 密钥支持环境变量覆盖，未设置时使用内置默认值（便于本地开发）：

| 变量名 | 默认值 | 说明 |
|---|---|---|
| `SP_DB_USERNAME` | `root` | 数据库账号 |
| `SP_DB_PASSWORD` | `admin` | 数据库密码（生产务必修改） |
| `SP_JWT_SECRET` | 配置文件内的示例值 | JWT 签名密钥（生产务必改成随机长字符串） |

```bash
# Linux / macOS
export SP_DB_PASSWORD='你的数据库密码'
export SP_JWT_SECRET='一串足够长的随机字符串'

# Windows PowerShell
$env:SP_DB_PASSWORD='你的数据库密码'
$env:SP_JWT_SECRET='一串足够长的随机字符串'
```

### 已安装实例升级（新增全员通知）

如果数据库是旧版初始化的（没有通知相关表），执行增量脚本即可（可重复执行）：

```bash
mysql -uroot -p < backend/src/main/resources/sql/notice.sql
```

该脚本会创建 `sp_notice` / `sp_notice_read` 两张表，并补充通知菜单与 `notice:*` 权限。全新安装无需执行，`init.sql` 已内置。

## 文档

- **用户操作手册**：[docs/USER_GUIDE.md](docs/USER_GUIDE.md)
- **开发者文档**：[docs/DEVELOPER_GUIDE.md](docs/DEVELOPER_GUIDE.md)

## 目录结构

```
shengnan-admin/
├── backend/    # Spring Boot 后端
│   └── src/main/resources/sql/   # init.sql（全量表）/ notice.sql（增量升级）
├── frontend/   # Vue 3 前端
├── docs/       # 用户手册 + 开发文档
├── LICENSE     # MIT 许可证
└── README.md
```

## 安全提示

定时任务支持执行 Shell 脚本与运行时编译 Java 代码，属于高危能力，受独立权限 `job:script` 保护，请仅授予可信管理员。生产环境务必修改 `application.yml` 中的 JWT 密钥与数据库密码，并将 `platform.cookie.secure` 改为 `true`（需 HTTPS 部署）。

认证令牌通过 `HttpOnly + SameSite` Cookie 下发，页面脚本无法读取，配合前端「HTML 转义 + 白名单标签生成」的 Markdown 渲染，形成针对 XSS 的纵深防御。

## 许可证

本项目采用 [MIT License](LICENSE) 开源，可自由用于个人与商业项目，无需保留除版权声明外的额外限制。
