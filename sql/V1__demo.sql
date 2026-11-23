-- ============================================================
-- 自定义 SQL 示例脚本
-- 文件名按字典序执行，建议 V{版本号}__{说明}.sql
-- 执行记录在 sp_sql_migration 表，执行成功后本脚本不会再跑第二次
-- 改本文件内容会导致启动校验失败（见 fail-on-checksum-mismatch）
-- 正确做法：新增 V2__xxx.sql，不要改已执行过的脚本
-- ============================================================

-- 示例 1：新增业务表（IF NOT EXISTS 保证幂等）
CREATE TABLE IF NOT EXISTS sp_custom_demo (
  id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  name       VARCHAR(128) NOT NULL                COMMENT '名称',
  remark     VARCHAR(255) DEFAULT ''              COMMENT '备注',
  is_deleted TINYINT(1)   DEFAULT 0               COMMENT '是否删除 1是 0否',
  created_at DATETIME     DEFAULT NULL            COMMENT '创建时间',
  updated_at DATETIME     DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义表示例';

-- 示例 2：给已有表加字段（MySQL 5.7 不支持 ADD COLUMN IF NOT EXISTS，
--          重复执行会报错，所以这类脚本只写一次，靠 sp_sql_migration 保证不重复）
-- ALTER TABLE sp_account ADD COLUMN nickname VARCHAR(64) DEFAULT '' COMMENT '昵称';

-- 示例 3：插入初始数据
INSERT INTO sp_custom_demo (name, remark, created_at)
SELECT 'demo', '首次启动自动写入', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sp_custom_demo WHERE name = 'demo');