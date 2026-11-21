-- ============================================================
-- 胜楠管理平台 - 全员通知功能增量脚本
-- 适用：已初始化的 shengnan_platform 库（MySQL 5.7+）
-- 特性：可重复执行（幂等）。全新安装请用 init.sql（已内置本功能）。
-- 变更：sp_notice 通知表 / sp_notice_read 已读记录表
--       系统管理 -> 通知公告菜单 + notice:* 动作权限
-- ============================================================
USE shengnan_platform;

-- ----------------------------
-- 1. 通知表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sp_notice (
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

-- ----------------------------
-- 2. 通知已读记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sp_notice_read (
  id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  notice_id  BIGINT      NOT NULL                COMMENT '通知ID',
  account_id BIGINT      NOT NULL                COMMENT '账号ID',
  read_time  DATETIME    DEFAULT NULL            COMMENT '阅读时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_notice_read (notice_id, account_id),
  KEY idx_notice_read_account (account_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='通知已读记录表';

-- ----------------------------
-- 3. 菜单与权限（不存在才插入，保证幂等）
-- ----------------------------
INSERT INTO sp_permission (id, parent_id, resource_name, resource_type, route_path, component, icon_name, permission, sort_no, is_visible, is_enabled)
SELECT 1108, 1100, '通知公告', 'menu', 'notice', 'system/notice/index', 'Bell', 'notice:list', 8, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sp_permission WHERE id = 1108);

INSERT INTO sp_permission (id, parent_id, resource_name, resource_type, route_path, component, icon_name, permission, sort_no, is_visible, is_enabled)
SELECT 110801, 1108, '查询通知', 'action', '', '', '', 'notice:list', 1, 0, 1
WHERE NOT EXISTS (SELECT 1 FROM sp_permission WHERE id = 110801);
INSERT INTO sp_permission (id, parent_id, resource_name, resource_type, route_path, component, icon_name, permission, sort_no, is_visible, is_enabled)
SELECT 110802, 1108, '新增通知', 'action', '', '', '', 'notice:create', 2, 0, 1
WHERE NOT EXISTS (SELECT 1 FROM sp_permission WHERE id = 110802);
INSERT INTO sp_permission (id, parent_id, resource_name, resource_type, route_path, component, icon_name, permission, sort_no, is_visible, is_enabled)
SELECT 110803, 1108, '编辑通知', 'action', '', '', '', 'notice:update', 3, 0, 1
WHERE NOT EXISTS (SELECT 1 FROM sp_permission WHERE id = 110803);
INSERT INTO sp_permission (id, parent_id, resource_name, resource_type, route_path, component, icon_name, permission, sort_no, is_visible, is_enabled)
SELECT 110804, 1108, '删除通知', 'action', '', '', '', 'notice:delete', 4, 0, 1
WHERE NOT EXISTS (SELECT 1 FROM sp_permission WHERE id = 110804);
INSERT INTO sp_permission (id, parent_id, resource_name, resource_type, route_path, component, icon_name, permission, sort_no, is_visible, is_enabled)
SELECT 110805, 1108, '发布撤回', 'action', '', '', '', 'notice:publish', 5, 0, 1
WHERE NOT EXISTS (SELECT 1 FROM sp_permission WHERE id = 110805);

-- 超级管理员拥有全部通知权限
INSERT INTO sp_role_permission (role_id, permission_id)
SELECT 1, id FROM sp_permission WHERE id IN (1108, 110801, 110802, 110803, 110804, 110805)
  AND NOT EXISTS (SELECT 1 FROM sp_role_permission rp WHERE rp.role_id = 1 AND rp.permission_id = sp_permission.id);

-- 访客（全员）拥有通知菜单查看权限：可见通知公告入口并查看已发布通知
-- 注意：查看/已读接口仅需登录，不校验 notice:list；此处授予仅为显示菜单
INSERT INTO sp_role_permission (role_id, permission_id)
SELECT 2, id FROM sp_permission WHERE id = 1108
  AND NOT EXISTS (SELECT 1 FROM sp_role_permission rp WHERE rp.role_id = 2 AND rp.permission_id = 1108);

SELECT '全员通知功能升级完成' AS msg;
