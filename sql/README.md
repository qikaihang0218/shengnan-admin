# 自定义 SQL

改 SQL 不用改代码、不用重新打包，**重启后自动执行**。

## 一、放哪里

```
shengnan-admin/
├── sql/                     ← 外置目录（推荐日常改这里）
│   └── V1__demo.sql
└── backend/
    └── src/main/resources/sql/migration/   ← 内置目录（随 jar 发布）
```

- 外置目录由 `platform.sql.script-dir` 配置，默认 `./sql,../sql`（两个候选，先找到哪个用哪个）
- 内置目录由 `platform.sql.classpath-dir` 配置，默认 `sql/migration`
- 同名脚本以外置目录为准

## 二、怎么写

文件名按**字典序**执行，建议 `V{版本}__{说明}.sql`：

```
V1__demo.sql
V2__add_account_nickname.sql
V3__create_report_table.sql
```

脚本内可以用：
- 多语句，用 `;` 分隔
- `--` 行注释、`#` 行注释、`/* */` 块注释
- 单引号/双引号字符串、反引号标识符（内含 `;` 不会被误切分）

## 三、执行规则

| 情况 | 行为 |
|---|---|
| 首次启动 | 按文件名顺序执行，成功后写入 `sp_sql_migration` |
| 再次启动 | 校验值一致 → 跳过 |
| 已执行过的脚本被改动 | 校验值不一致 → **启动失败**并提示（防止静默漏执行） |
| 某条语句报错 | 中断启动，日志打印文件名 + 第几条 + 出错 SQL |

执行记录表：

```sql
SELECT * FROM sp_sql_migration;
```

## 四、配置项（application.yml）

```yaml
platform:
  sql:
    enabled: true                       # 总开关，生产可关
    script-dir: ./sql,../sql            # 外置目录，逗号分隔多个候选
    classpath-dir: sql/migration        # 内置目录
    fail-on-checksum-mismatch: true     # 脚本改动导致校验不一致时中断启动
    fail-fast: true                     # 任一脚本失败中断启动
```

## 五、注意

1. **不要改已执行过的脚本**。要调整就新建 `V2__xxx.sql`。
   如果确实要重跑，删掉记录再重启：
   ```sql
   DELETE FROM sp_sql_migration WHERE script_name = 'V1__demo.sql';
   ```
2. **MySQL 5.7 不支持** `ADD COLUMN IF NOT EXISTS`，这类语句只写一次，靠执行记录保证不重复。
   建表尽量用 `CREATE TABLE IF NOT EXISTS`，插数据用 `INSERT ... SELECT ... WHERE NOT EXISTS`。
3. 生产环境若不想自动执行，设 `platform.sql.enabled: false`，改为人工执行。
4. `init.sql` 含 `DROP TABLE`，**不要**放进 `sql/` 目录，只用于首次建库。