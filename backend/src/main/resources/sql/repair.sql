-- ============================================================
-- 胜楠管理平台 - 报修模块增量脚本
-- 适用：已初始化的 shengnan_platform 库（MySQL 5.7+）
-- 特性：可重复执行（幂等）。全新安装请用 init.sql 后再执行本脚本。
-- 变更：sp_repair_order 报修工单表 / sp_repair_log 工单流转日志表
--       报修管理目录 + repair:* 动作权限 + 报修类别字典
-- ============================================================
USE shengnan_platform;

-- ----------------------------
-- 1. 报修工单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sp_repair_order (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_no       VARCHAR(32)  NOT NULL                COMMENT '工单号',
  title          VARCHAR(200) NOT NULL                COMMENT '报修标题',
  category       VARCHAR(32)  DEFAULT NULL            COMMENT '报修类别（字典 sys_repair_category）',
  priority       TINYINT(1)   DEFAULT 1               COMMENT '优先级 0低 1中 2高 3紧急',
  status         TINYINT(1)   DEFAULT 0               COMMENT '状态 0待受理 1已派单 2处理中 3已完成 4已关闭',
  location       VARCHAR(200) DEFAULT NULL            COMMENT '报修地点',
  description    TEXT                                 COMMENT '故障描述',
  contact_name   VARCHAR(64)  DEFAULT NULL            COMMENT '联系人',
  contact_phone  VARCHAR(32)  DEFAULT NULL            COMMENT '联系电话',
  applicant_id   BIGINT       DEFAULT NULL            COMMENT '报修人账号ID',
  applicant_name VARCHAR(64)  DEFAULT NULL            COMMENT '报修人姓名',
  dept_id        BIGINT       DEFAULT NULL            COMMENT '报修部门ID',
  handler_id     BIGINT       DEFAULT NULL            COMMENT '处理人账号ID',
  handler_name   VARCHAR(64)  DEFAULT NULL            COMMENT '处理人姓名',
  expect_time    DATETIME     DEFAULT NULL            COMMENT '期望完成时间',
  assign_time    DATETIME     DEFAULT NULL            COMMENT '派单时间',
  finish_time    DATETIME     DEFAULT NULL            COMMENT '完成时间',
  result_desc    VARCHAR(500) DEFAULT NULL            COMMENT '处理结果',
  rating         TINYINT(1)   DEFAULT NULL            COMMENT '满意度 1-5',
  remark         VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  is_deleted     TINYINT(1)   DEFAULT 0               COMMENT '是否删除',
  created_by     VARCHAR(64)  DEFAULT ''              COMMENT '创建人',
  created_at     DATETIME     DEFAULT NULL            COMMENT '创建时间',
  updated_by     VARCHAR(64)  DEFAULT ''              COMMENT '更新人',
  updated_at     DATETIME     DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_repair_order_no (order_no),
  KEY idx_repair_status (status, is_deleted),
  KEY idx_repair_applicant (applicant_id),
  KEY idx_repair_handler (handler_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='报修工单表';

-- 1.1 故障照片列（老库补列，幂等）
SET @has_images := (SELECT COUNT(*) FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sp_repair_order' AND COLUMN_NAME = 'images');
SET @ddl := IF(@has_images = 0,
  'ALTER TABLE sp_repair_order ADD COLUMN images VARCHAR(1000) DEFAULT NULL COMMENT ''故障照片（文件ID，逗号分隔）'' AFTER description',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 2. 工单流转日志表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sp_repair_log (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_id    BIGINT      NOT NULL                COMMENT '工单ID',
  action      VARCHAR(32) DEFAULT NULL            COMMENT '动作 创建/派单/开始处理/完工/关闭/驳回',
  from_status TINYINT(1)  DEFAULT NULL            COMMENT '变更前状态',
  to_status   TINYINT(1)  DEFAULT NULL            COMMENT '变更后状态',
  operator    VARCHAR(64) DEFAULT NULL            COMMENT '操作人',
  content     VARCHAR(500) DEFAULT NULL           COMMENT '说明',
  created_at  DATETIME    DEFAULT NULL            COMMENT '操作时间',
  PRIMARY KEY (id),
  KEY idx_repair_log_order (order_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='报修工单流转日志';

-- ----------------------------
-- 3. 菜单与权限（不存在才插入，保证幂等）
-- ----------------------------
INSERT INTO sp_permission (id, parent_id, resource_name, resource_type, route_path, component, icon_name, permission, sort_no, is_visible, is_enabled, is_deleted, created_at)
SELECT 1300, 0, '报修管理', 'catalog', '/repair', 'Layout', 'Tools', '', 3, 1, 1, 0, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sp_permission WHERE id = 1300);

INSERT INTO sp_permission (id, parent_id, resource_name, resource_type, route_path, component, icon_name, permission, sort_no, is_visible, is_enabled, is_deleted, created_at)
SELECT 1301, 1300, '报修工单', 'menu', 'order', 'repair/order/index', 'EditPen', 'repair:list', 1, 1, 1, 0, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sp_permission WHERE id = 1301);

INSERT INTO sp_permission (id, parent_id, resource_name, resource_type, route_path, component, icon_name, permission, sort_no, is_visible, is_enabled, is_deleted, created_at)
SELECT 130101, 1301, '查询工单', 'action', '', '', '', 'repair:list', 1, 0, 1, 0, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sp_permission WHERE id = 130101);

INSERT INTO sp_permission (id, parent_id, resource_name, resource_type, route_path, component, icon_name, permission, sort_no, is_visible, is_enabled, is_deleted, created_at)
SELECT 130102, 1301, '提交报修', 'action', '', '', '', 'repair:create', 2, 0, 1, 0, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sp_permission WHERE id = 130102);

INSERT INTO sp_permission (id, parent_id, resource_name, resource_type, route_path, component, icon_name, permission, sort_no, is_visible, is_enabled, is_deleted, created_at)
SELECT 130103, 1301, '编辑工单', 'action', '', '', '', 'repair:update', 3, 0, 1, 0, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sp_permission WHERE id = 130103);

INSERT INTO sp_permission (id, parent_id, resource_name, resource_type, route_path, component, icon_name, permission, sort_no, is_visible, is_enabled, is_deleted, created_at)
SELECT 130104, 1301, '删除工单', 'action', '', '', '', 'repair:delete', 4, 0, 1, 0, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sp_permission WHERE id = 130104);

INSERT INTO sp_permission (id, parent_id, resource_name, resource_type, route_path, component, icon_name, permission, sort_no, is_visible, is_enabled, is_deleted, created_at)
SELECT 130105, 1301, '派单处理', 'action', '', '', '', 'repair:assign', 5, 0, 1, 0, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sp_permission WHERE id = 130105);

INSERT INTO sp_permission (id, parent_id, resource_name, resource_type, route_path, component, icon_name, permission, sort_no, is_visible, is_enabled, is_deleted, created_at)
SELECT 130106, 1301, '完工关闭', 'action', '', '', '', 'repair:finish', 6, 0, 1, 0, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sp_permission WHERE id = 130106);

-- 超级管理员拥有新增资源
INSERT INTO sp_role_permission (role_id, permission_id)
SELECT 1, p.id FROM sp_permission p
WHERE p.id IN (1300, 1301, 130101, 130102, 130103, 130104, 130105, 130106)
  AND NOT EXISTS (SELECT 1 FROM sp_role_permission rp WHERE rp.role_id = 1 AND rp.permission_id = p.id);

-- 部门管理员/研发人员/审计员：可查看与处理工单
INSERT INTO sp_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sp_role r, sp_permission p
WHERE r.role_code IN ('dept_admin', 'developer', 'auditor')
  AND p.id IN (1300, 1301, 130101, 130102, 130103, 130105, 130106)
  AND NOT EXISTS (SELECT 1 FROM sp_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- 人事专员：可提交/查看
INSERT INTO sp_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sp_role r, sp_permission p
WHERE r.role_code = 'hr'
  AND p.id IN (1300, 1301, 130101, 130102)
  AND NOT EXISTS (SELECT 1 FROM sp_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ----------------------------
-- 4. 报修类别字典
-- ----------------------------
INSERT INTO sp_dict_type (id, dict_code, dict_name, description, is_enabled, is_deleted, created_at)
SELECT 10, 'sys_repair_category', '报修类别', '报修工单可选类别', 1, 0, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sp_dict_type WHERE dict_code = 'sys_repair_category');

INSERT INTO sp_dict_item (type_id, item_label, item_value, sort_no, is_enabled, is_deleted, created_at)
SELECT t.id, d.label, d.val, d.sort, 1, 0, NOW()
FROM sp_dict_type t
JOIN (
  SELECT '水电维修' AS label, 'water_power' AS val, 1 AS sort
  UNION ALL SELECT '网络故障', 'network', 2
  UNION ALL SELECT '办公设备', 'device', 3
  UNION ALL SELECT '空调暖通', 'hvac', 4
  UNION ALL SELECT '门锁门窗', 'door', 5
  UNION ALL SELECT '其他', 'other', 6
) d
WHERE t.dict_code = 'sys_repair_category'
  AND NOT EXISTS (SELECT 1 FROM sp_dict_item i WHERE i.type_id = t.id AND i.item_value = d.val);

-- ----------------------------
-- 5. 演示工单（按工单号去重，可重复执行）
-- ----------------------------
INSERT INTO sp_repair_order (order_no, title, category, priority, status, location, description,
  contact_name, contact_phone, applicant_id, applicant_name, dept_id, handler_id, handler_name,
  expect_time, assign_time, finish_time, result_desc, rating, is_deleted, created_by, created_at)
SELECT 'BX20260101001', '三楼会议室空调不制冷', 'hvac', 2, 2, '总部三楼 301 会议室',
  '空调开机后出风为常温，室内温度持续升高，影响会议使用。', '张伟', '13800000001',
  (SELECT id FROM sp_account WHERE username = 'zhangsan' LIMIT 1), 'zhangsan', 4,
  (SELECT id FROM sp_account WHERE username = 'lisi' LIMIT 1), '李四',
  DATE_ADD(NOW(), INTERVAL 2 DAY), NOW(), NULL, NULL, NULL, 0, 'zhangsan', DATE_SUB(NOW(), INTERVAL 2 HOUR)
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sp_repair_order WHERE order_no = 'BX20260101001');

INSERT INTO sp_repair_order (order_no, title, category, priority, status, location, description,
  contact_name, contact_phone, applicant_id, applicant_name, dept_id, handler_id, handler_name,
  expect_time, assign_time, finish_time, result_desc, rating, is_deleted, created_by, created_at)
SELECT 'BX20260101002', '办公区网络频繁掉线', 'network', 3, 3, '总部五楼 前端研发组',
  '无线网络每 10 分钟断连一次，影响日常开发。', '王五', '13800000002',
  (SELECT id FROM sp_account WHERE username = 'wangwu' LIMIT 1), 'wangwu', 13,
  (SELECT id FROM sp_account WHERE username = 'zhaoliu' LIMIT 1), '赵六',
  DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), NOW(),
  '更换无线路由器并调整信道，网络已恢复稳定。', 5, 0, 'wangwu', DATE_SUB(NOW(), INTERVAL 4 DAY)
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sp_repair_order WHERE order_no = 'BX20260101002');

INSERT INTO sp_repair_order (order_no, title, category, priority, status, location, description,
  contact_name, contact_phone, applicant_id, applicant_name, dept_id, is_deleted, created_by, created_at)
SELECT 'BX20260101003', '二楼卫生间水龙头漏水', 'water_power', 1, 0, '总部二楼卫生间',
  '水龙头关闭后仍持续滴水，地面湿滑存在安全隐患。', '孙七', '13800000003',
  (SELECT id FROM sp_account WHERE username = 'sunqi' LIMIT 1), 'sunqi', 5,
  0, 'sunqi', DATE_SUB(NOW(), INTERVAL 30 MINUTE)
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sp_repair_order WHERE order_no = 'BX20260101003');

INSERT INTO sp_repair_order (order_no, title, category, priority, status, location, description,
  contact_name, contact_phone, applicant_id, applicant_name, dept_id, handler_id, handler_name,
  assign_time, finish_time, result_desc, rating, is_deleted, created_by, created_at)
SELECT 'BX20260101004', '打印机无法进纸', 'device', 0, 4, '总部四楼 综合管理部',
  '激光打印机卡纸后无法进纸，已自行排查未解决。', '周八', '13800000004',
  (SELECT id FROM sp_account WHERE username = 'zhouba' LIMIT 1), 'zhouba', 5,
  (SELECT id FROM sp_account WHERE username = 'wujiu' LIMIT 1), '吴九',
  DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY),
  '更换搓纸轮后恢复正常，已交付使用。', 4, 0, 'zhouba', DATE_SUB(NOW(), INTERVAL 11 DAY)
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sp_repair_order WHERE order_no = 'BX20260101004');

-- ----------------------------
-- 6. 演示流转日志（按工单+动作去重）
-- ----------------------------
INSERT INTO sp_repair_log (order_id, action, from_status, to_status, operator, content, created_at)
SELECT o.id, '创建', NULL, 0, o.applicant_name, '提交报修申请', o.created_at
FROM sp_repair_order o
WHERE NOT EXISTS (SELECT 1 FROM sp_repair_log l WHERE l.order_id = o.id AND l.action = '创建');

INSERT INTO sp_repair_log (order_id, action, from_status, to_status, operator, content, created_at)
SELECT o.id, '派单', 0, 1, 'admin', CONCAT('指派给 ', o.handler_name), o.assign_time
FROM sp_repair_order o
WHERE o.handler_id IS NOT NULL AND o.assign_time IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sp_repair_log l WHERE l.order_id = o.id AND l.action = '派单');

INSERT INTO sp_repair_log (order_id, action, from_status, to_status, operator, content, created_at)
SELECT o.id, '开始处理', 1, 2, o.handler_name, '处理人开始处理', o.assign_time
FROM sp_repair_order o
WHERE o.status = 2
  AND NOT EXISTS (SELECT 1 FROM sp_repair_log l WHERE l.order_id = o.id AND l.action = '开始处理');

INSERT INTO sp_repair_log (order_id, action, from_status, to_status, operator, content, created_at)
SELECT o.id, '完工', 2, 3, o.handler_name, o.result_desc, o.finish_time
FROM sp_repair_order o
WHERE o.status = 3 AND o.finish_time IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sp_repair_log l WHERE l.order_id = o.id AND l.action = '完工');

INSERT INTO sp_repair_log (order_id, action, from_status, to_status, operator, content, created_at)
SELECT o.id, '关闭', 3, 4, 'admin', '验收通过，关闭工单', o.finish_time
FROM sp_repair_order o
WHERE o.status = 4 AND o.finish_time IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sp_repair_log l WHERE l.order_id = o.id AND l.action = '关闭');

SELECT '报修模块初始化完成' AS msg;