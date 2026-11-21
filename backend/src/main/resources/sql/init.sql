-- ============================================================
-- 胜楠管理平台 - 数据库初始化脚本
-- 数据库: shengnan_platform  |  MySQL 5.7+  |  utf8mb4
-- 命名约定：表前缀 sp_；主键统一 id；审计字段 created_at/updated_at；
--          逻辑删除 is_deleted(0/1)；状态 is_enabled(0/1)；
--          权限标识 "资源:动作"；资源类型 catalog/menu/action
-- 默认账号 admin / admin123 （密码见下方 BCrypt）
-- ============================================================

CREATE DATABASE IF NOT EXISTS shengnan_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE shengnan_platform;

-- ----------------------------
-- 1. 账号表
-- ----------------------------
DROP TABLE IF EXISTS sp_account;
CREATE TABLE sp_account (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username      VARCHAR(64)  NOT NULL                COMMENT '登录账号',
  password_hash VARCHAR(128) NOT NULL                COMMENT '密码(BCrypt)',
  display_name  VARCHAR(64)  DEFAULT ''              COMMENT '显示名称',
  avatar_url    VARCHAR(255) DEFAULT ''              COMMENT '头像地址',
  email         VARCHAR(128) DEFAULT ''              COMMENT '邮箱',
  mobile        VARCHAR(20)  DEFAULT ''              COMMENT '手机号',
  gender        TINYINT(1)   DEFAULT 0               COMMENT '性别 0未知 1男 2女',
  department_id BIGINT       DEFAULT NULL            COMMENT '所属部门ID',
  is_enabled    TINYINT(1)   DEFAULT 1               COMMENT '是否启用 1是 0否',
  is_deleted    TINYINT(1)   DEFAULT 0               COMMENT '是否删除 1是 0否',
  created_by    VARCHAR(64)  DEFAULT ''              COMMENT '创建人',
  created_at    DATETIME     DEFAULT NULL            COMMENT '创建时间',
  updated_by    VARCHAR(64)  DEFAULT ''              COMMENT '更新人',
  updated_at    DATETIME     DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_account_username (username),
  KEY idx_account_dept (department_id)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='账号表';

-- ----------------------------
-- 2. 角色表
-- ----------------------------
DROP TABLE IF EXISTS sp_role;
CREATE TABLE sp_role (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  role_code   VARCHAR(64)  NOT NULL                COMMENT '角色编码',
  role_name   VARCHAR(64)  NOT NULL                COMMENT '角色名称',
  sort_no     INT          DEFAULT 0               COMMENT '排序号',
  data_scope  VARCHAR(16)  DEFAULT 'self'          COMMENT '数据范围 all/dept/dept_tree/self',
  is_enabled  TINYINT(1)   DEFAULT 1               COMMENT '是否启用',
  is_deleted  TINYINT(1)   DEFAULT 0               COMMENT '是否删除',
  created_by    VARCHAR(64)  DEFAULT ''   COMMENT '创建人',
  created_at    DATETIME     DEFAULT NULL    COMMENT '创建时间',
  updated_by    VARCHAR(64)  DEFAULT ''   COMMENT '更新人',
  updated_at    DATETIME     DEFAULT NULL    COMMENT '更新时间',
  description VARCHAR(500) DEFAULT ''              COMMENT '角色说明',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ----------------------------
-- 3. 权限资源表（目录/菜单/动作统一建模）
-- ----------------------------
DROP TABLE IF EXISTS sp_permission;
CREATE TABLE sp_permission (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  parent_id     BIGINT       DEFAULT 0               COMMENT '父级ID，0为根',
  resource_name VARCHAR(64)  NOT NULL                COMMENT '资源名称',
  resource_type VARCHAR(16)  NOT NULL                COMMENT '类型 catalog目录/menu菜单/action动作',
  route_path    VARCHAR(200) DEFAULT ''              COMMENT '前端路由地址',
  component     VARCHAR(255) DEFAULT ''              COMMENT '前端组件路径',
  icon_name     VARCHAR(100) DEFAULT ''              COMMENT '图标名称',
  permission    VARCHAR(128) DEFAULT ''              COMMENT '权限标识，如 account:create',
  sort_no       INT          DEFAULT 0               COMMENT '排序号',
  is_visible    TINYINT(1)   DEFAULT 1               COMMENT '是否可见',
  is_enabled    TINYINT(1)   DEFAULT 1               COMMENT '是否启用',
  is_deleted    TINYINT(1)   DEFAULT 0               COMMENT '是否删除',
  created_by    VARCHAR(64)  DEFAULT ''              COMMENT '创建人',
  created_at    DATETIME     DEFAULT NULL            COMMENT '创建时间',
  updated_by    VARCHAR(64)  DEFAULT ''              COMMENT '更新人',
  updated_at    DATETIME     DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_perm_parent (parent_id)
) ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8mb4 COMMENT='权限资源表';

-- ----------------------------
-- 4. 账号-角色关联
-- ----------------------------
DROP TABLE IF EXISTS sp_account_role;
CREATE TABLE sp_account_role (
  account_id BIGINT NOT NULL COMMENT '账号ID',
  role_id    BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (account_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号角色关联';

-- ----------------------------
-- 5. 角色-权限关联
-- ----------------------------
DROP TABLE IF EXISTS sp_role_permission;
CREATE TABLE sp_role_permission (
  role_id       BIGINT NOT NULL COMMENT '角色ID',
  permission_id BIGINT NOT NULL COMMENT '权限资源ID',
  PRIMARY KEY (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联';

-- ----------------------------
-- 6. 部门表
-- ----------------------------
DROP TABLE IF EXISTS sp_department;
CREATE TABLE sp_department (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  parent_id   BIGINT       DEFAULT 0               COMMENT '父级ID',
  dept_name   VARCHAR(64)  NOT NULL                COMMENT '部门名称',
  dept_code   VARCHAR(64)  DEFAULT ''              COMMENT '部门编码',
  leader_name VARCHAR(64)  DEFAULT ''              COMMENT '负责人',
  contact     VARCHAR(64)  DEFAULT ''              COMMENT '联系方式',
  sort_no     INT          DEFAULT 0               COMMENT '排序号',
  is_enabled  TINYINT(1)   DEFAULT 1,
  is_deleted  TINYINT(1)   DEFAULT 0,
  created_by    VARCHAR(64)  DEFAULT ''   COMMENT '创建人',
  created_at    DATETIME     DEFAULT NULL    COMMENT '创建时间',
  updated_by    VARCHAR(64)  DEFAULT ''   COMMENT '更新人',
  updated_at    DATETIME     DEFAULT NULL    COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_dept_parent (parent_id)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- ----------------------------
-- 7. 岗位表
-- ----------------------------
DROP TABLE IF EXISTS sp_position;
CREATE TABLE sp_position (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  position_name VARCHAR(64) NOT NULL               COMMENT '岗位名称',
  position_code VARCHAR(64) NOT NULL               COMMENT '岗位编码',
  sort_no     INT          DEFAULT 0,
  is_enabled  TINYINT(1)   DEFAULT 1,
  is_deleted  TINYINT(1)   DEFAULT 0,
  created_by    VARCHAR(64)  DEFAULT ''   COMMENT '创建人',
  created_at    DATETIME     DEFAULT NULL    COMMENT '创建时间',
  updated_by    VARCHAR(64)  DEFAULT ''   COMMENT '更新人',
  updated_at    DATETIME     DEFAULT NULL    COMMENT '更新时间',
  description VARCHAR(500) DEFAULT '',
  PRIMARY KEY (id),
  UNIQUE KEY uk_position_code (position_code)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='岗位表';

-- ----------------------------
-- 8. 字典类型
-- ----------------------------
DROP TABLE IF EXISTS sp_dict_type;
CREATE TABLE sp_dict_type (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  dict_code   VARCHAR(64)  NOT NULL                COMMENT '字典编码',
  dict_name   VARCHAR(64)  NOT NULL                COMMENT '字典名称',
  is_enabled  TINYINT(1)   DEFAULT 1,
  is_deleted  TINYINT(1)   DEFAULT 0               COMMENT '是否删除',
  created_by    VARCHAR(64)  DEFAULT ''            COMMENT '创建人',
  created_at    DATETIME     DEFAULT NULL    COMMENT '创建时间',
  updated_by    VARCHAR(64)  DEFAULT ''   COMMENT '更新人',
  updated_at    DATETIME     DEFAULT NULL    COMMENT '更新时间',
  description VARCHAR(500) DEFAULT '',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dicttype_code (dict_code)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

-- ----------------------------
-- 9. 字典项
-- ----------------------------
DROP TABLE IF EXISTS sp_dict_item;
CREATE TABLE sp_dict_item (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  type_id     BIGINT       NOT NULL                COMMENT '字典类型ID',
  item_label  VARCHAR(64)  NOT NULL                COMMENT '显示文本',
  item_value  VARCHAR(64)  NOT NULL                COMMENT '存储值',
  sort_no     INT          DEFAULT 0,
  is_enabled  TINYINT(1)   DEFAULT 1,
  is_deleted  TINYINT(1)   DEFAULT 0               COMMENT '是否删除',
  created_by    VARCHAR(64)  DEFAULT ''            COMMENT '创建人',
  created_at    DATETIME     DEFAULT NULL    COMMENT '创建时间',
  updated_by    VARCHAR(64)  DEFAULT ''   COMMENT '更新人',
  updated_at    DATETIME     DEFAULT NULL    COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_dictitem_type (type_id)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='字典项表';

-- ----------------------------
-- 10. 系统参数
-- ----------------------------
DROP TABLE IF EXISTS sp_config;
CREATE TABLE sp_config (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  config_key  VARCHAR(128) NOT NULL                COMMENT '参数键',
  config_value VARCHAR(500) DEFAULT ''             COMMENT '参数值',
  config_name VARCHAR(64)  DEFAULT ''              COMMENT '参数名称',
  is_builtin  TINYINT(1)   DEFAULT 0               COMMENT '是否内置 1不可删',
  is_deleted  TINYINT(1)   DEFAULT 0               COMMENT '是否删除',
  created_by    VARCHAR(64)  DEFAULT ''            COMMENT '创建人',
  created_at    DATETIME     DEFAULT NULL    COMMENT '创建时间',
  updated_by    VARCHAR(64)  DEFAULT ''   COMMENT '更新人',
  updated_at    DATETIME     DEFAULT NULL    COMMENT '更新时间',
  description VARCHAR(500) DEFAULT '',
  PRIMARY KEY (id),
  UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='系统参数表';

-- ----------------------------
-- 11. 登录记录
-- ----------------------------
DROP TABLE IF EXISTS sp_login_log;
CREATE TABLE sp_login_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username    VARCHAR(64)  DEFAULT ''              COMMENT '登录账号',
  ip_addr     VARCHAR(64)  DEFAULT ''              COMMENT 'IP地址',
  user_agent  VARCHAR(500) DEFAULT ''              COMMENT '浏览器标识',
  os_name     VARCHAR(64)  DEFAULT ''              COMMENT '操作系统',
  is_success  TINYINT(1)   DEFAULT 1               COMMENT '是否成功',
  message     VARCHAR(255) DEFAULT ''              COMMENT '结果说明',
  login_at    DATETIME     DEFAULT NULL            COMMENT '登录时间',
  PRIMARY KEY (id),
  KEY idx_loginlog_time (login_at)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='登录记录表';

-- ----------------------------
-- 12. 操作日志
-- ----------------------------
DROP TABLE IF EXISTS sp_operation_log;
CREATE TABLE sp_operation_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  module_name VARCHAR(64)  DEFAULT ''              COMMENT '功能模块',
  action_desc VARCHAR(128) DEFAULT ''              COMMENT '操作描述',
  method_name VARCHAR(200) DEFAULT ''              COMMENT '请求方法',
  request_uri VARCHAR(500) DEFAULT ''              COMMENT '请求地址',
  params_json TEXT                                 COMMENT '请求参数',
  ip_addr     VARCHAR(64)  DEFAULT ''              COMMENT 'IP地址',
  operator    VARCHAR(64)  DEFAULT ''              COMMENT '操作人',
  cost_ms     INT          DEFAULT 0               COMMENT '耗时毫秒',
  is_success  TINYINT(1)   DEFAULT 1,
  error_msg   VARCHAR(500) DEFAULT '',
  operated_at DATETIME     DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_oplog_time (operated_at)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ----------------------------
-- 13. 文件对象
-- ----------------------------
DROP TABLE IF EXISTS sp_file;
CREATE TABLE sp_file (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  original_name VARCHAR(255) NOT NULL               COMMENT '原始文件名',
  object_key   VARCHAR(255) NOT NULL                COMMENT '存储对象键',
  storage_path VARCHAR(500) NOT NULL                COMMENT '磁盘存储路径',
  content_type VARCHAR(128) DEFAULT ''              COMMENT '媒体类型',
  size_bytes   BIGINT       DEFAULT 0               COMMENT '字节数',
  uploader     VARCHAR(64)  DEFAULT ''              COMMENT '上传者',
  is_deleted   TINYINT(1)   DEFAULT 0,
  uploaded_at  DATETIME     DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_file_key (object_key)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='文件对象表';

-- ----------------------------
-- 14. 定时任务
-- ----------------------------
DROP TABLE IF EXISTS sp_job;
CREATE TABLE sp_job (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  job_name      VARCHAR(64)  NOT NULL                COMMENT '任务名称',
  job_group     VARCHAR(64)  DEFAULT 'DEFAULT'       COMMENT '任务分组',
  job_kind      VARCHAR(16)  DEFAULT 'bean'          COMMENT '类型 bean/shell/java',
  invoke_target VARCHAR(500) DEFAULT ''              COMMENT '调用目标(bean类型)',
  script_body   TEXT                                 COMMENT '脚本内容(shell/java类型)',
  cron_expr     VARCHAR(64)  NOT NULL                COMMENT 'cron表达式(6位)',
  allow_concurrent TINYINT(1) DEFAULT 0              COMMENT '是否允许并发',
  is_enabled    TINYINT(1)   DEFAULT 1               COMMENT '是否启用',
  is_deleted    TINYINT(1)   DEFAULT 0,
  created_by    VARCHAR(64)  DEFAULT ''   COMMENT '创建人',
  created_at    DATETIME     DEFAULT NULL    COMMENT '创建时间',
  updated_by    VARCHAR(64)  DEFAULT ''   COMMENT '更新人',
  updated_at    DATETIME     DEFAULT NULL    COMMENT '更新时间',
  description   VARCHAR(500) DEFAULT '',
  PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='定时任务表';

-- ----------------------------
-- 15. 任务运行日志
-- ----------------------------
DROP TABLE IF EXISTS sp_job_log;
CREATE TABLE sp_job_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  job_id      BIGINT       DEFAULT NULL            COMMENT '任务ID',
  job_name    VARCHAR(64)  DEFAULT ''              COMMENT '任务名称',
  job_kind    VARCHAR(16)  DEFAULT ''              COMMENT '任务类型',
  target_brief VARCHAR(300) DEFAULT ''             COMMENT '调用目标摘要',
  is_success  TINYINT(1)   DEFAULT 1               COMMENT '是否成功',
  log_message VARCHAR(2000) DEFAULT ''             COMMENT '执行信息',
  cost_ms     INT          DEFAULT 0,
  fired_at    DATETIME     DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_joblog_job (job_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='任务运行日志表';

-- ----------------------------
-- 16. 令牌黑名单
-- ----------------------------
DROP TABLE IF EXISTS sp_token_blocklist;
CREATE TABLE sp_token_blocklist (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  token_digest VARCHAR(64) NOT NULL                COMMENT '令牌摘要(SHA-256)',
  expires_at  DATETIME     NOT NULL                COMMENT '过期时间',
  created_at  DATETIME     DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_token_digest (token_digest),
  KEY idx_blocklist_expire (expires_at)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='令牌黑名单表';

-- ----------------------------
-- 17. 全员通知
-- ----------------------------
DROP TABLE IF EXISTS sp_notice;
CREATE TABLE sp_notice (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  title        VARCHAR(200) NOT NULL                COMMENT '通知标题',
  content      TEXT                                 COMMENT '通知内容',
  notice_type  TINYINT(1)   DEFAULT 1               COMMENT '类型 1通知 2公告',
  is_top       TINYINT(1)   DEFAULT 0               COMMENT '是否置顶 1是 0否',
  status       TINYINT(1)   DEFAULT 0               COMMENT '状态 0草稿 1已发布',
  publish_time DATETIME     DEFAULT NULL            COMMENT '发布时间',
  is_deleted   TINYINT(1)   DEFAULT 0               COMMENT '是否删除',
  created_by    VARCHAR(64)  DEFAULT ''             COMMENT '创建人',
  created_at    DATETIME     DEFAULT NULL           COMMENT '创建时间',
  updated_by    VARCHAR(64)  DEFAULT ''             COMMENT '更新人',
  updated_at    DATETIME     DEFAULT NULL           COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_notice_status_top (status, is_top, publish_time)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='全员通知表';

DROP TABLE IF EXISTS sp_notice_read;
CREATE TABLE sp_notice_read (
  id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  notice_id  BIGINT      NOT NULL                COMMENT '通知ID',
  account_id BIGINT      NOT NULL                COMMENT '账号ID',
  read_time  DATETIME    DEFAULT NULL            COMMENT '阅读时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_notice_read (notice_id, account_id),
  KEY idx_notice_read_account (account_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='通知已读记录表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 账号 admin / admin123  (BCrypt 已验证)
INSERT INTO sp_account (id, username, password_hash, display_name, email, is_enabled, is_deleted, created_by, created_at)
VALUES (1, 'admin', '$2a$10$Q2t9KFdunXYlpY6UOqpiiuklbWkFiO.2TkXCQq5.oXGf/szER92va', '平台管理员', 'admin@shengnan.cn', 1, 0, 'system', NOW());

-- 角色：超级管理员 / 访客
INSERT INTO sp_role (id, role_code, role_name, sort_no, data_scope, is_enabled, is_deleted, created_at, description) VALUES
(1, 'super_admin', '超级管理员', 1, 'all',  1, 0, NOW(), '拥有全部权限'),
(2, 'guest',       '访客',       2, 'self', 1, 0, NOW(), '只读权限');

INSERT INTO sp_account_role (account_id, role_id) VALUES (1, 1);

-- 权限资源树：目录 / 菜单 / 动作
INSERT INTO sp_permission (id, parent_id, resource_name, resource_type, route_path, component, icon_name, permission, sort_no, is_visible, is_enabled) VALUES
-- 工作台
(1000, 0, '工作台', 'menu', '/dashboard', 'dashboard/index', 'DataBoard', '', 0, 1, 1),
-- 系统管理目录
(1100, 0, '系统管理', 'catalog', '/system', 'Layout', 'Setting', '', 1, 1, 1),
(1101, 1100, '账号管理', 'menu', 'account', 'system/account/index', 'User', 'account:list', 1, 1, 1),
(110101, 1101, '查询账号', 'action', '', '', '', 'account:list', 1, 0, 1),
(110102, 1101, '新增账号', 'action', '', '', '', 'account:create', 2, 0, 1),
(110103, 1101, '编辑账号', 'action', '', '', '', 'account:update', 3, 0, 1),
(110104, 1101, '删除账号', 'action', '', '', '', 'account:delete', 4, 0, 1),
(110105, 1101, '重置密码', 'action', '', '', '', 'account:reset-pwd', 5, 0, 1),
(1102, 1100, '角色管理', 'menu', 'role', 'system/role/index', 'UserFilled', 'role:list', 2, 1, 1),
(110201, 1102, '查询角色', 'action', '', '', '', 'role:list', 1, 0, 1),
(110202, 1102, '新增角色', 'action', '', '', '', 'role:create', 2, 0, 1),
(110203, 1102, '编辑角色', 'action', '', '', '', 'role:update', 3, 0, 1),
(110204, 1102, '删除角色', 'action', '', '', '', 'role:delete', 4, 0, 1),
(110205, 1102, '分配权限', 'action', '', '', '', 'role:grant', 5, 0, 1),
(1103, 1100, '权限资源', 'menu', 'permission', 'system/permission/index', 'Grid', 'permission:list', 3, 1, 1),
(110301, 1103, '查询资源', 'action', '', '', '', 'permission:list', 1, 0, 1),
(110302, 1103, '新增资源', 'action', '', '', '', 'permission:create', 2, 0, 1),
(110303, 1103, '编辑资源', 'action', '', '', '', 'permission:update', 3, 0, 1),
(110304, 1103, '删除资源', 'action', '', '', '', 'permission:delete', 4, 0, 1),
(1104, 1100, '部门管理', 'menu', 'department', 'system/department/index', 'OfficeBuilding', 'department:list', 4, 1, 1),
(110401, 1104, '查询部门', 'action', '', '', '', 'department:list', 1, 0, 1),
(110402, 1104, '新增部门', 'action', '', '', '', 'department:create', 2, 0, 1),
(110403, 1104, '编辑部门', 'action', '', '', '', 'department:update', 3, 0, 1),
(110404, 1104, '删除部门', 'action', '', '', '', 'department:delete', 4, 0, 1),
(1105, 1100, '岗位管理', 'menu', 'position', 'system/position/index', 'Briefcase', 'position:list', 5, 1, 1),
(110501, 1105, '查询岗位', 'action', '', '', '', 'position:list', 1, 0, 1),
(110502, 1105, '新增岗位', 'action', '', '', '', 'position:create', 2, 0, 1),
(110503, 1105, '编辑岗位', 'action', '', '', '', 'position:update', 3, 0, 1),
(110504, 1105, '删除岗位', 'action', '', '', '', 'position:delete', 4, 0, 1),
(1106, 1100, '字典管理', 'menu', 'dictionary', 'system/dictionary/index', 'Collection', 'dict:list', 6, 1, 1),
(110601, 1106, '查询字典', 'action', '', '', '', 'dict:list', 1, 0, 1),
(110602, 1106, '维护字典', 'action', '', '', '', 'dict:write', 2, 0, 1),
(1107, 1100, '系统参数', 'menu', 'config', 'system/config/index', 'Tools', 'config:list', 7, 1, 1),
(110701, 1107, '查询参数', 'action', '', '', '', 'config:list', 1, 0, 1),
(110702, 1107, '修改参数', 'action', '', '', '', 'config:update', 2, 0, 1),
-- 通知公告
(1108, 1100, '通知公告', 'menu', 'notice', 'system/notice/index', 'Bell', 'notice:list', 8, 1, 1),
(110801, 1108, '查询通知', 'action', '', '', '', 'notice:list', 1, 0, 1),
(110802, 1108, '新增通知', 'action', '', '', '', 'notice:create', 2, 0, 1),
(110803, 1108, '编辑通知', 'action', '', '', '', 'notice:update', 3, 0, 1),
(110804, 1108, '删除通知', 'action', '', '', '', 'notice:delete', 4, 0, 1),
(110805, 1108, '发布撤回', 'action', '', '', '', 'notice:publish', 5, 0, 1),
-- 系统监控目录
(1200, 0, '系统监控', 'catalog', '/monitor', 'Layout', 'Monitor', '', 2, 1, 1),
(1201, 1200, '登录记录', 'menu', 'login-log', 'monitor/login-log/index', 'Document', 'login-log:list', 1, 1, 1),
(120101, 1201, '查询登录记录', 'action', '', '', '', 'login-log:list', 1, 0, 1),
(120102, 1201, '清空登录记录', 'action', '', '', '', 'login-log:clean', 2, 0, 1),
(1202, 1200, '操作日志', 'menu', 'operation-log', 'monitor/operation-log/index', 'Tickets', 'operation-log:list', 2, 1, 1),
(120201, 1202, '查询操作日志', 'action', '', '', '', 'operation-log:list', 1, 0, 1),
(120202, 1202, '清空操作日志', 'action', '', '', '', 'operation-log:clean', 2, 0, 1),
(1203, 1200, '定时任务', 'menu', 'job', 'monitor/job/index', 'Clock', 'job:list', 3, 1, 1),
(120301, 1203, '查询任务', 'action', '', '', '', 'job:list', 1, 0, 1),
(120302, 1203, '新增任务', 'action', '', '', '', 'job:create', 2, 0, 1),
(120303, 1203, '编辑任务', 'action', '', '', '', 'job:update', 3, 0, 1),
(120304, 1203, '删除任务', 'action', '', '', '', 'job:delete', 4, 0, 1),
(120305, 1203, '执行任务', 'action', '', '', '', 'job:run', 5, 0, 1),
(120306, 1203, '查询日志', 'action', '', '', '', 'job:log-list', 6, 0, 1),
(120307, 1203, '脚本任务', 'action', '', '', '', 'job:script', 7, 0, 1),
(1204, 1200, '文件管理', 'menu', 'file', 'monitor/file/index', 'FolderOpened', 'file:list', 4, 1, 1),
(120401, 1204, '查询文件', 'action', '', '', '', 'file:list', 1, 0, 1),
(120402, 1204, '上传文件', 'action', '', '', '', 'file:upload', 2, 0, 1),
(120403, 1204, '下载文件', 'action', '', '', '', 'file:download', 3, 0, 1),
(120404, 1204, '删除文件', 'action', '', '', '', 'file:delete', 4, 0, 1);

-- 超级管理员拥有全部资源
INSERT INTO sp_role_permission (role_id, permission_id)
SELECT 1, id FROM sp_permission;

-- 访客只拥有工作台 + 菜单查看 + 通知公告（全员可见）
INSERT INTO sp_role_permission (role_id, permission_id)
SELECT 2, id FROM sp_permission WHERE resource_type IN ('catalog','menu') OR permission IN ('account:list','role:list');

-- 示例字典
INSERT INTO sp_dict_type (id, dict_code, dict_name, description, created_at) VALUES
(1, 'sys_gender', '性别', '用户性别', NOW()),
(2, 'sys_yes_no', '是否', '通用是否', NOW());
INSERT INTO sp_dict_item (type_id, item_label, item_value, sort_no, created_at) VALUES
(1, '男', '1', 1, NOW()), (1, '女', '2', 2, NOW()), (1, '未知', '0', 3, NOW()),
(2, '是', '1', 1, NOW()), (2, '否', '0', 2, NOW());

-- 示例参数
INSERT INTO sp_config (config_key, config_value, config_name, is_builtin, created_at) VALUES
('platform.name', '胜楠管理平台', '平台名称', 1, NOW()),
('platform.version', '1.0.0', '平台版本', 1, NOW()),
('login.max-attempts', '5', '登录失败次数上限', 0, NOW());

SELECT '胜楠管理平台数据库初始化完成' AS msg;
