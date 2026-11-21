-- ============================================================
-- 胜楠管理平台 - 演示基础数据（增量脚本，可重复执行）
-- 适用：已执行 init.sql 的 shengnan_platform 库（MySQL 5.7+）
-- 说明：全部使用显式主键 + INSERT IGNORE，重复执行不会产生脏数据
-- 演示账号统一密码：admin123
-- ============================================================
USE shengnan_platform;

-- ----------------------------
-- 1. 部门（三级组织架构）
-- ----------------------------
INSERT IGNORE INTO sp_department
  (id, parent_id, dept_name, dept_code, leader_name, contact, sort_no, is_enabled, is_deleted, created_by, created_at)
VALUES
  (1, 0, '胜楠科技',       'SN',        '张伟',   '021-88001000', 1, 1, 0, 'system', NOW()),
  (2, 1, '技术研发中心',   'SN-RD',     '李强',   '021-88001100', 1, 1, 0, 'system', NOW()),
  (3, 2, '平台研发部',     'SN-RD-PF',  '王芳',   '021-88001110', 1, 1, 0, 'system', NOW()),
  (4, 3, '后端研发组',     'SN-RD-PF-BE', '赵敏', '021-88001111', 1, 1, 0, 'system', NOW()),
  (5, 3, '前端研发组',     'SN-RD-PF-FE', '孙杰', '021-88001112', 2, 1, 0, 'system', NOW()),
  (6, 2, '质量保障部',     'SN-RD-QA',  '周涛',   '021-88001120', 2, 1, 0, 'system', NOW()),
  (7, 2, '运维保障部',     'SN-RD-OPS', '吴磊',   '021-88001130', 3, 1, 0, 'system', NOW()),
  (8, 1, '市场营销中心',   'SN-MKT',    '郑爽',   '021-88001200', 2, 1, 0, 'system', NOW()),
  (9, 8, '华东大区',       'SN-MKT-EC', '冯雷',   '021-88001210', 1, 1, 0, 'system', NOW()),
  (10, 8, '华南大区',      'SN-MKT-SC', '陈静',   '021-88001220', 2, 1, 0, 'system', NOW()),
  (11, 1, '综合管理部',    'SN-GA',     '褚辉',   '021-88001300', 3, 1, 0, 'system', NOW()),
  (12, 11, '人力资源部',   'SN-GA-HR',  '卫红',   '021-88001310', 1, 1, 0, 'system', NOW()),
  (13, 11, '财务部',       'SN-GA-FIN', '蒋楠',   '021-88001320', 2, 1, 0, 'system', NOW());

-- ----------------------------
-- 2. 岗位
-- ----------------------------
INSERT IGNORE INTO sp_position
  (id, position_name, position_code, sort_no, is_enabled, is_deleted, created_by, created_at, description)
VALUES
  (1,  '总经理',       'GM',   1,  1, 0, 'system', NOW(), '负责公司整体经营管理'),
  (2,  '技术总监',       'CTO',  2,  1, 0, 'system', NOW(), '统筹技术路线与研发团队'),
  (3,  '产品经理',       'PM',   3,  1, 0, 'system', NOW(), '负责产品规划与需求管理'),
  (4,  '高级开发工程师', 'SE',   4,  1, 0, 'system', NOW(), '核心模块设计与开发'),
  (5,  '前端开发工程师', 'FE',   5,  1, 0, 'system', NOW(), '前端界面与交互实现'),
  (6,  '测试工程师',     'QA',   6,  1, 0, 'system', NOW(), '功能测试与质量把关'),
  (7,  '运维工程师',     'OPS',  7,  1, 0, 'system', NOW(), '环境部署与线上运维'),
  (8,  '市场专员',       'MKT',  8,  1, 0, 'system', NOW(), '市场推广与客户拓展'),
  (9,  '人事专员',       'HR',   9,  1, 0, 'system', NOW(), '招聘、考勤与员工关系'),
  (10, '财务专员',       'FIN',  10, 1, 0, 'system', NOW(), '账务处理与费用报销');

-- ----------------------------
-- 3. 角色（1 超管 / 2 访客 由 init.sql 创建）
-- ----------------------------
INSERT IGNORE INTO sp_role
  (id, role_code, role_name, sort_no, data_scope, is_enabled, is_deleted, created_at, description)
VALUES
  (3, 'dept_admin', '部门管理员', 3, 'dept_tree', 1, 0, NOW(), '管理本部门及下级部门数据'),
  (4, 'developer',  '研发人员',   4, 'self',      1, 0, NOW(), '研发团队日常使用'),
  (5, 'hr',         '人事专员',   5, 'dept',      1, 0, NOW(), '维护账号与组织架构'),
  (6, 'auditor',    '审计员',     6, 'all',       1, 0, NOW(), '只读查看全部数据与日志');

-- ----------------------------
-- 4. 账号（密码统一 admin123）
-- ----------------------------
INSERT IGNORE INTO sp_account
  (id, username, password_hash, display_name, email, mobile, gender, department_id,
   is_enabled, is_deleted, created_by, created_at)
VALUES
  (2, 'zhangsan', '$2a$10$Q2t9KFdunXYlpY6UOqpiiuklbWkFiO.2TkXCQq5.oXGf/szER92va', '张三', 'zhangsan@shengnan.cn', '13800001001', 1, 3,  1, 0, 'system', NOW()),
  (3, 'lisi',     '$2a$10$Q2t9KFdunXYlpY6UOqpiiuklbWkFiO.2TkXCQq5.oXGf/szER92va', '李四', 'lisi@shengnan.cn',     '13800001002', 2, 4,  1, 0, 'system', NOW()),
  (4, 'wangwu',   '$2a$10$Q2t9KFdunXYlpY6UOqpiiuklbWkFiO.2TkXCQq5.oXGf/szER92va', '王五', 'wangwu@shengnan.cn',   '13800001003', 1, 5,  1, 0, 'system', NOW()),
  (5, 'zhaoliu',  '$2a$10$Q2t9KFdunXYlpY6UOqpiiuklbWkFiO.2TkXCQq5.oXGf/szER92va', '赵六', 'zhaoliu@shengnan.cn',  '13800001004', 2, 6,  1, 0, 'system', NOW()),
  (6, 'sunqi',    '$2a$10$Q2t9KFdunXYlpY6UOqpiiuklbWkFiO.2TkXCQq5.oXGf/szER92va', '孙七', 'sunqi@shengnan.cn',    '13800001005', 1, 7,  1, 0, 'system', NOW()),
  (7, 'zhouba',   '$2a$10$Q2t9KFdunXYlpY6UOqpiiuklbWkFiO.2TkXCQq5.oXGf/szER92va', '周八', 'zhouba@shengnan.cn',   '13800001006', 2, 9,  1, 0, 'system', NOW()),
  (8, 'wujiu',    '$2a$10$Q2t9KFdunXYlpY6UOqpiiuklbWkFiO.2TkXCQq5.oXGf/szER92va', '吴九', 'wujiu@shengnan.cn',    '13800001007', 1, 12, 1, 0, 'system', NOW()),
  (9, 'zhengshi', '$2a$10$Q2t9KFdunXYlpY6UOqpiiuklbWkFiO.2TkXCQq5.oXGf/szER92va', '郑十', 'zhengshi@shengnan.cn', '13800001008', 2, 13, 0, 0, 'system', NOW());

-- 账号-角色（admin 由 init.sql 绑定超管）
INSERT IGNORE INTO sp_account_role (account_id, role_id) VALUES
  (2, 3),
  (3, 4),
  (4, 4),
  (5, 4),
  (6, 4),
  (7, 3),
  (8, 5),
  (9, 6);

-- ----------------------------
-- 5. 字典类型与字典项
-- ----------------------------
INSERT IGNORE INTO sp_dict_type (id, dict_code, dict_name, description, created_at) VALUES
  (1, 'sys_gender',      '性别',     '用户性别',             NOW()),
  (2, 'sys_yes_no',      '是否',     '通用是/否',            NOW()),
  (3, 'sys_user_status', '账号状态', '账号启用状态',         NOW()),
  (4, 'sys_notice_type', '通知类型', '通知公告的类型',       NOW()),
  (5, 'sys_job_group',   '任务分组', '定时任务所属分组',     NOW()),
  (6, 'sys_data_scope',  '数据范围', '角色的数据可见范围',   NOW()),
  (7, 'sys_industry',    '所属行业', '客户所属行业分类',     NOW()),
  (8, 'sys_edu_level',   '学历',     '员工最高学历',         NOW()),
  (9, 'sys_file_type',   '文件类型', '上传文件常见类型',     NOW());

INSERT IGNORE INTO sp_dict_item (id, type_id, item_label, item_value, sort_no, is_enabled, is_deleted, created_at) VALUES
  (1,  1, '男',   '1', 1, 1, 0, NOW()),
  (2,  1, '女',   '2', 2, 1, 0, NOW()),
  (3,  1, '未知', '0', 3, 1, 0, NOW()),
  (4,  2, '是',   '1', 1, 1, 0, NOW()),
  (5,  2, '否',   '0', 2, 1, 0, NOW()),
  (6,  3, '启用', '1', 1, 1, 0, NOW()),
  (7,  3, '停用', '0', 2, 1, 0, NOW()),
  (8,  4, '通知', '1', 1, 1, 0, NOW()),
  (9,  4, '公告', '2', 2, 1, 0, NOW()),
  (10, 4, '警告', '3', 3, 1, 0, NOW()),
  (11, 5, '默认分组', 'DEFAULT', 1, 1, 0, NOW()),
  (12, 5, '系统任务', 'SYSTEM',  2, 1, 0, NOW()),
  (13, 5, '业务任务', 'BUSINESS', 3, 1, 0, NOW()),
  (14, 6, '全部数据',     'all',       1, 1, 0, NOW()),
  (15, 6, '本部门',       'dept',      2, 1, 0, NOW()),
  (16, 6, '本部门及以下', 'dept_tree', 3, 1, 0, NOW()),
  (17, 6, '仅本人',       'self',      4, 1, 0, NOW()),
  (18, 7, '互联网',   'IT',  1, 1, 0, NOW()),
  (19, 7, '金融',     'FIN', 2, 1, 0, NOW()),
  (20, 7, '制造业',   'MFG', 3, 1, 0, NOW()),
  (21, 7, '医疗健康', 'MED', 4, 1, 0, NOW()),
  (22, 7, '教育培训', 'EDU', 5, 1, 0, NOW()),
  (23, 8, '大专',   '1', 1, 1, 0, NOW()),
  (24, 8, '本科',   '2', 2, 1, 0, NOW()),
  (25, 8, '硕士',   '3', 3, 1, 0, NOW()),
  (26, 8, '博士',   '4', 4, 1, 0, NOW()),
  (27, 9, '文档',   'doc',  1, 1, 0, NOW()),
  (28, 9, '表格',   'xls',  2, 1, 0, NOW()),
  (29, 9, '图片',   'img',  3, 1, 0, NOW()),
  (30, 9, '压缩包', 'zip',  4, 1, 0, NOW()),
  (31, 9, '其他',   'other', 5, 1, 0, NOW());

-- ----------------------------
-- 6. 系统参数
-- ----------------------------
INSERT IGNORE INTO sp_config (config_key, config_value, config_name, is_builtin, is_deleted, created_at, description) VALUES
  ('platform.name',        '胜楠管理平台', '平台名称',         1, 0, NOW(), '显示在浏览器标题与登录页'),
  ('platform.version',     '1.0.0',        '平台版本',         1, 0, NOW(), '当前部署版本号'),
  ('platform.copyright',   '© 2024 胜楠科技', '版权信息',      1, 0, NOW(), '页脚版权声明'),
  ('platform.icp',         '沪ICP备2024000001号', '备案号',    0, 0, NOW(), '网站备案信息'),
  ('login.max-attempts',   '5',            '登录失败次数上限', 0, 0, NOW(), '超过后锁定账号'),
  ('login.lock-minutes',   '15',           '账号锁定时长(分钟)', 0, 0, NOW(), '锁定后自动解锁时间'),
  ('login.captcha-enabled','true',         '启用登录验证码',   0, 0, NOW(), 'true/false'),
  ('password.min-length',  '8',            '密码最小长度',     0, 0, NOW(), '重置密码时的校验规则'),
  ('password.expire-days', '90',           '密码有效期(天)',   0, 0, NOW(), '0 表示永不过期'),
  ('upload.max-size-mb',   '20',           '单文件大小上限(MB)', 0, 0, NOW(), '超过则拒绝上传'),
  ('upload.allow-ext',     'jpg,png,pdf,docx,xlsx,zip', '允许上传的扩展名', 0, 0, NOW(), '逗号分隔'),
  ('session.timeout-min',  '1440',         '登录会话超时(分钟)', 0, 0, NOW(), '对应 JWT 有效期'),
  ('log.retain-days',      '90',           '日志保留天数',     0, 0, NOW(), '超期日志由定时任务清理'),
  ('notice.page-size',     '10',           '通知列表每页条数', 0, 0, NOW(), '前端分页默认值');

-- ----------------------------
-- 7. 全员通知
-- ----------------------------
INSERT IGNORE INTO sp_notice
  (id, title, content, notice_type, is_top, status, publish_time, is_deleted, created_by, created_at)
VALUES
  (1, '胜楠管理平台正式上线',
      '胜楠管理平台 1.0.0 已正式上线，包含账号、角色、权限、部门、岗位、字典、参数、通知、日志、定时任务与文件管理等模块。如在使用中遇到问题，请联系综合管理部。',
      2, 1, 1, DATE_SUB(NOW(), INTERVAL 7 DAY), 0, 'admin', DATE_SUB(NOW(), INTERVAL 7 DAY)),
  (2, '关于加强账号安全管理的通知',
      '请各位同事及时修改初始密码，密码长度不少于 8 位并包含字母与数字；离开工位时请退出登录，切勿将账号借予他人使用。',
      1, 1, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), 0, 'admin', DATE_SUB(NOW(), INTERVAL 5 DAY)),
  (3, '系统例行维护公告',
      '平台将于本周六 22:00 - 24:00 进行例行维护，期间可能短暂无法访问，请提前保存工作内容。',
      2, 0, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), 0, 'admin', DATE_SUB(NOW(), INTERVAL 3 DAY)),
  (4, '新功能上线：定时任务与文件管理',
      '本次更新新增定时任务（支持 bean / shell / java 三种类型）与文件管理模块，可在「系统监控」菜单下使用。',
      1, 0, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), 0, 'admin', DATE_SUB(NOW(), INTERVAL 2 DAY)),
  (5, '十月考勤统计说明',
      '十月考勤已统计完毕，请各部门负责人于本月 10 日前完成核对，如有异议请联系人力资源部。',
      1, 0, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), 0, 'admin', DATE_SUB(NOW(), INTERVAL 1 DAY)),
  (6, '关于规范文件上传的通知（草稿）',
      '上传文件请使用规范命名，避免上传与工作无关的大体积文件。本通知待完善后发布。',
      1, 0, 0, NULL, 0, 'admin', NOW());

-- ----------------------------
-- 8. 定时任务
-- ----------------------------
INSERT IGNORE INTO sp_job
  (id, job_name, job_group, job_kind, invoke_target, script_body, cron_expr,
   allow_concurrent, is_enabled, is_deleted, created_by, created_at, description)
VALUES
  (1, '清理过期令牌', 'SYSTEM', 'bean', 'systemTasks.cleanBlocklist()', NULL,
      '0 0 3 * * ?', 0, 1, 0, 'admin', NOW(), '每天凌晨 3 点清理已过期的令牌黑名单'),
  (2, '服务心跳检测', 'SYSTEM', 'bean', 'systemTasks.heartbeat()', NULL,
      '0 0/10 * * * ?', 0, 1, 0, 'admin', NOW(), '每 10 分钟执行一次，用于验证调度器可用'),
  (3, '打印服务器时间', 'DEFAULT', 'shell', NULL, 'echo server time: %date% %time%',
      '0 0/30 * * * ?', 0, 0, 0, 'admin', NOW(), '示例 Shell 任务，默认停用'),
  (4, 'Java 代码示例', 'BUSINESS', 'java', NULL,
      'return "hello from java job";',
      '0 0 8 ? * MON', 0, 0, 0, 'admin', NOW(), '示例 Java 任务，每周一 8 点执行，默认停用');

-- ----------------------------
-- 9. 角色权限（部门管理员 / 研发人员 / 人事专员 / 审计员）
-- ----------------------------
-- 部门管理员：全部菜单 + 常用增删改查动作
INSERT IGNORE INTO sp_role_permission (role_id, permission_id)
SELECT 3, id FROM sp_permission
WHERE is_deleted = 0
  AND (resource_type IN ('catalog', 'menu')
       OR permission IN ('account:list', 'account:create', 'account:update',
                         'role:list', 'department:list', 'department:create', 'department:update',
                         'position:list', 'dict:list', 'notice:list', 'notice:create',
                         'login-log:list', 'operation-log:list', 'file:list', 'file:upload'));

-- 研发人员：工作台 + 系统监控只读
INSERT IGNORE INTO sp_role_permission (role_id, permission_id)
SELECT 4, id FROM sp_permission
WHERE is_deleted = 0
  AND (resource_type IN ('catalog', 'menu')
       OR permission IN ('account:list', 'department:list', 'position:list', 'dict:list',
                         'login-log:list', 'operation-log:list', 'job:list', 'job:log-list',
                         'file:list', 'file:upload', 'file:download'));

-- 人事专员：账号与组织架构维护
INSERT IGNORE INTO sp_role_permission (role_id, permission_id)
SELECT 5, id FROM sp_permission
WHERE is_deleted = 0
  AND (resource_type IN ('catalog', 'menu')
       OR permission IN ('account:list', 'account:create', 'account:update', 'account:reset-pwd',
                         'role:list', 'department:list', 'department:create', 'department:update', 'department:delete',
                         'position:list', 'position:create', 'position:update',
                         'dict:list', 'notice:list', 'notice:create',
                         'login-log:list', 'operation-log:list'));

-- 审计员：全部只读（list / log-list / download）
INSERT IGNORE INTO sp_role_permission (role_id, permission_id)
SELECT 6, id FROM sp_permission
WHERE is_deleted = 0
  AND (resource_type IN ('catalog', 'menu')
       OR permission LIKE '%:list' OR permission LIKE '%:log-list'
       OR permission IN ('file:download'));

SELECT '演示基础数据初始化完成' AS msg;