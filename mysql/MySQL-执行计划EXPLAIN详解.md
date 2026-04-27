<file_path> interview-bible\mysql\MySQL-执行计划EXPLAIN详解.md </file_path>
<edit_description> 创建MySQL执行计划EXPLAIN详解文档 </edit_description>

# MySQL 执行计划 EXPLAIN 详解

> 本文档全面讲解 MySQL EXPLAIN 命令的使用方法、输出字段含义及优化技巧，帮助开发者快速定位 SQL 性能问题。

---

## 目录

1. [EXPLAIN 基础](#1-explain-基础)
2. [EXPLAIN 输出格式](#2-explain-输出格式)
3. [核心字段详解](#3-核心字段详解)
4. [type 字段详解](#4-type-字段详解)
5. [Extra 字段详解](#5-extra-字段详解)
6. [常见 SQL 执行计划分析](#6-常见-sql-执行计划分析)
7. [优化案例分析](#7-优化案例分析)
8. [EXPLAIN ANALYZE](#8-explain-analyze)

---

## 1. EXPLAIN 基础

### 1.1 什么是 EXPLAIN？

EXPLAIN 是 MySQL 提供的分析查询执行计划的工具，可以显示 MySQL 如何执行 SQL 语句，包括：

- 表的读取顺序
- 使用哪些索引
- 访问类型（全表扫描/索引扫描等）
- 扫描的行数估算
- 是否使用临时表/文件排序等

### 1.2 基本语法

```sql
-- 基本用法
EXPLAIN SELECT * FROM users WHERE id = 1;

-- 查看详细执行计划
EXPLAIN FORMAT=JSON SELECT * FROM users WHERE id = 1;

-- 查看执行计划并执行（MySQL 8.0.18+）
EXPLAIN ANALYZE SELECT * FROM users WHERE id = 1;

-- 查看 UPDATE/DELETE 执行计划
EXPLAIN UPDATE users SET age = 20 WHERE id = 1;
EXPLAIN DELETE FROM users WHERE id = 1;
```

### 1.3 EXPLAIN 输出列一览

| 字段 | 含义 |
|------|------|
| **id** | SELECT 标识符，标识执行顺序 |
| **select_type** | SELECT 类型 |
| **table** | 访问的表名 |
| **partitions** | 匹配的分区（分区表） |
| **type** | 访问类型（重要！） |
| **possible_keys** | 可能使用的索引 |
| **key** | 实际使用的索引 |
| **key_len** | 使用的索引长度 |
| **ref** | 与索引比较的列或常量 |
| **rows** | 估计扫描的行数 |
| **filtered** | 按条件过滤后剩余行的百分比 |
| **Extra** | 额外信息（重要！） |

---

## 2. EXPLAIN 输出格式

### 2.1 传统表格格式

```sql
EXPLAIN SELECT * FROM users u 
JOIN orders o ON u.id = o.user_id 
WHERE u.age > 18;
```

```
+----+-------------+-------+------------+--------+---------------+---------+---------+-------------------+------+----------+-------------+
| id | select_type | table | partitions | type   | possible_keys | key     | key_len | ref               | rows | filtered | Extra       |
+----+-------------+-------+------------+--------+---------------+---------+---------+-------------------+------+----------+-------------+
|  1 | SIMPLE      | u     | NULL       | ALL    | PRIMARY       | NULL    | NULL    | NULL              | 1000 |    33.33 | Using where |
|  1 | SIMPLE      | o     | NULL       | ref    | idx_user_id   | idx_user_id | 8     | test.u.id         |   10 |   100.00 | NULL        |
+----+-------------+-------+------------+--------+---------------+---------+---------+-------------------+------+----------+-------------+
```

### 2.2 JSON 格式（MySQL 5.6+）

```sql
EXPLAIN FORMAT=JSON SELECT * FROM users WHERE id = 1;
```

更详细的成本估算和优化过程信息。

### 2.3 TREE 格式（MySQL 8.0.16+）

```sql
EXPLAIN FORMAT=TREE SELECT * FROM users WHERE id = 1;
```

以树形结构展示执行计划，更容易理解执行顺序。

---

## 3. 核心字段详解

### 3.1 id 字段：执行顺序标识

```sql
EXPLAIN SELECT * FROM t1 WHERE id IN (
    SELECT t2.id FROM t2 WHERE t2.name = 'xxx'
);
```

| id 含义 | 说明 |
|---------|------|
| **id 相同** | 从上往下顺序执行 |
| **id 不同** | 数值越大优先级越高，越先执行 |
| **id 为 NULL** | 最后执行（通常表示 UNION 结果） |

```sql
-- 多表 JOIN，id 相同
EXPLAIN SELECT * FROM t1, t2, t3 WHERE t1.id = t2.id AND t2.id = t3.id;
-- id 都是 1，执行顺序：t1 → t2 → t3

-- 子查询，id 不同
EXPLAIN SELECT * FROM t1 WHERE id = (SELECT id FROM t2 WHERE name = 'x');
-- id=1 (t1) 和 id=2 (t2)，先执行 t2 (id=2)，再执行 t1 (id=1)
```

### 3.2 select_type 字段：查询类型

| 类型 | 说明 | 示例 |
|------|------|------|
| **SIMPLE** | 简单查询，不含子查询或 UNION | `SELECT * FROM t1` |
| **PRIMARY** | 最外层查询 | 包含子查询的外层 |
| **SUBQUERY** | SELECT 子查询（非 FROM 子句） | `SELECT * FROM t1 WHERE id IN (SELECT ...)` |
| **DERIVED** | FROM 子句中的子查询 | `SELECT * FROM (SELECT ...) t` |
| **UNION** | UNION 中的第二个及以后查询 | `SELECT ... UNION SELECT ...` |
| **UNION RESULT** | UNION 的结果 | UNION 合并后的结果 |
| **MATERIALIZED** | 物化子查询（MySQL 5.6+） | IN 子查询优化为物化表 |
| **DEPENDENT SUBQUERY** | 依赖外部查询的子查询 | 相关子查询 |

### 3.3 table 字段：访问的表

```sql
-- 显示表名
-- <derivedN>：派生表，N 是子查询的 id
-- <unionM,N>：UNION 结果
-- <subqueryN>：物化子查询
```

### 3.4 possible_keys：可能使用的索引

显示查询可以使用的所有索引，但实际不一定使用。

- 为 NULL 表示没有可用索引
- 有值但 key 为 NULL 表示索引未被选中（可能因优化器认为全表扫描更快）

### 3.5 key：实际使用的索引

- NULL：未使用索引
- 具体索引名：使用了该索引
- **索引選択是优化器根据成本估算决定的**

### 3.6 key_len：索引使用长度

表示 MySQL 实际使用的索引字节数，可用于判断索引利用率。

```sql
-- 假设 idx_name_age 是 (name, age) 联合索引
-- key_len = name(20) + age(4) = 24，表示使用了完整的联合索引

-- 如果只使用 name 列
-- key_len = 20
```

**关键提示**：key_len 越短越好，但要确保满足查询需求。

### 3.7 rows：估算扫描行数

- MySQL 根据索引统计信息估算需要扫描的行数
- **该值越小越好**
- 是估算值，可能与实际有偏差

### 3.8 filtered：过滤百分比

```sql
-- 表示按条件过滤后剩余行的百分比
-- rows × filtered% = 实际返回给服务器的行数估算
-- 越接近 100% 越好
```

---

## 4. type 字段详解（重点！）

type 表示 **表的访问类型/连接类型**，是判断 SQL 性能的重要指标。

**性能从好到差排序：**

```
system > const > eq_ref > ref > range > index > ALL
（越靠前性能越好）
```

### 4.1 system：系统表（最佳）

表中只有一行数据（系统表），是 const 的特例。

```sql
EXPLAIN SELECT * FROM mysql.tables_priv LIMIT 1;
-- type = system
```

### 4.2 const：常量（极佳）

通过主键或唯一索引一次就能找到记录。

```sql
EXPLAIN SELECT * FROM users WHERE id = 1;
-- id 是主键，type = const
```

### 4.3 eq_ref：唯一索引连接（优秀）

JOIN 时使用主键或唯一索引关联，对于每个前面的表记录，只返回一条记录。

```sql
EXPLAIN SELECT * FROM users u 
JOIN orders o ON u.id = o.user_id;
-- u.id 是主键，o.user_id 有唯一索引
-- users: ALL, orders: eq_ref
```

### 4.4 ref：非唯一索引（良好）

使用非唯一索引或前缀索引，可能返回多条记录。

```sql
EXPLAIN SELECT * FROM users WHERE name = '张三';
-- name 有普通索引，type = ref
```

### 4.5 range：范围扫描（一般）

使用索引进行范围查询。

```sql
EXPLAIN SELECT * FROM users WHERE age BETWEEN 18 AND 30;
EXPLAIN SELECT * FROM users WHERE id > 100;
EXPLAIN SELECT * FROM users WHERE name LIKE '张%';
-- type = range
```

### 4.6 index：索引全扫描（较差）

遍历整个索引树，比 ALL 快，但仍需优化。

```sql
-- 覆盖索引查询
EXPLAIN SELECT name FROM users ORDER BY name;
-- 只需要 name 列，且 name 有索引
-- 直接从索引树获取，无需访问数据行
```

### 4.7 ALL：全表扫描（最差）

遍历整个表，性能最差，大数据量时应避免。

```sql
EXPLAIN SELECT * FROM users WHERE age = 20;
-- age 没有索引，type = ALL
```

### 4.8 type 对比总结

| type | 说明 | 性能 | 优化建议 |
|------|------|------|---------|
| const/eq_ref | 主键/唯一索引 | ⭐⭐⭐⭐⭐ | 理想状态 |
| ref | 普通索引 | ⭐⭐⭐⭐ | 良好 |
| range | 范围查询 | ⭐⭐⭐ | 避免大范围 |
| index | 索引全扫描 | ⭐⭐ | 检查是否必要 |
| ALL | 全表扫描 | ⭐ | 必须优化 |

---

## 5. Extra 字段详解（重点！）

Extra 显示 SQL 执行的额外信息，是优化的重要依据。

### 5.1 Using index（好）

**覆盖索引**：查询的列都在索引中，无需回表。

```sql
-- 表结构：users(id, name, age)，索引 idx_name_age(name, age)
EXPLAIN SELECT name, age FROM users WHERE name = '张三';
-- Extra: Using index
```

### 5.2 Using where

使用 WHERE 条件过滤数据。

- 在 **存储引擎层** 过滤（好）
- 在 **Server 层** 过滤（需优化）

```sql
-- 存储引擎层过滤（好）
EXPLAIN SELECT * FROM users WHERE age > 18;
-- age 有索引，直接在存储引擎过滤

-- Server 层过滤（需优化）
EXPLAIN SELECT * FROM users WHERE YEAR(create_time) = 2024;
-- 函数导致索引失效，Server 层过滤
```

### 5.3 Using temporary（坏）

使用了临时表，通常由 ORDER BY 或 GROUP BY 引起。

```sql
EXPLAIN SELECT DISTINCT age FROM users ORDER BY name;
-- Extra: Using temporary; Using filesort
-- 建议：确保 ORDER BY/GROUP BY 使用索引
```

### 5.4 Using filesort（坏）

MySQL 需要额外排序，而不是使用索引顺序。

```sql
EXPLAIN SELECT * FROM users ORDER BY age;
-- age 没有索引，需要 filesort
-- 优化：给 age 加索引
```

### 5.5 Using join buffer（JOIN 优化）

使用了连接缓存，通常在 JOIN 时没有使用索引。

```sql
-- 优化：确保 JOIN 条件有索引
```

### 5.6 Using index condition（ICP，好）

索引条件下推（Index Condition Pushdown），MySQL 5.6+ 优化。

```sql
-- 在存储引擎层过滤数据，减少回表
EXPLAIN SELECT * FROM users WHERE name LIKE '张%' AND age = 20;
-- 即使 age 在联合索引的第二位，也能部分利用索引
```

### 5.7 Using MRR（Multi-Range Read）

多范围读优化，减少随机磁盘读取。

```sql
-- 将随机 I/O 转换为顺序 I/O
```

### 5.8 Select tables optimized away

对于 MIN/MAX 查询，优化器可以直接从索引获取结果。

```sql
EXPLAIN SELECT MIN(id) FROM users;
-- Extra: Select tables optimized away
```

### 5.9 Impossible WHERE

WHERE 条件永远为 false，优化器直接跳过查询。

```sql
EXPLAIN SELECT * FROM users WHERE id = 1 AND id = 2;
-- Extra: Impossible WHERE
```

---

## 6. 常见 SQL 执行计划分析

### 6.1 单表查询

```sql
-- 示例 1：使用主键
EXPLAIN SELECT * FROM users WHERE id = 1;
/*
+----+-------------+-------+------------+-------+---------------+---------+---------+-------+------+----------+-------+
| id | select_type | table | type       | key   | key_len       | ref     | rows    | Extra |
+----+-------------+-------+------------+-------+---------------+---------+---------+-------+------+----------+-------+
|  1 | SIMPLE      | users | const      | PRIMARY | 8             | const   | 1       | NULL  |
+----+-------------+-------+------------+-------+---------------+---------+---------+-------+------+----------+-------+
分析：
- type=const：使用主键，性能最佳
- rows=1：只扫描1行
- Extra=NULL：无额外操作
*/

-- 示例 2：使用普通索引
EXPLAIN SELECT * FROM users WHERE email = 'xxx@xxx.com';
/*
+----+-------------+-------+------+---------------+------+---------+-------+------+-------------+
| id | select_type | table | type | possible_keys | key  | key_len | ref   | rows | Extra       |
+----+-------------+-------+------+---------------+------+---------+-------+------+-------------+
|  1 | SIMPLE      | users | ref  | idx_email     | idx_email | 303 | const | 1    | Using index condition |
+----+-------------+-------+------+---------------+------+---------+-------+------+-------------+
分析：
- type=ref：使用非唯一索引
- key=idx_email：实际使用 email 索引
- rows=1：估计扫描1行
- Extra=Using index condition：使用 ICP 优化
*/

-- 示例 3：索引失效（函数）
EXPLAIN SELECT * FROM users WHERE DATE(create_time) = '2024-01-01';
/*
+----+-------------+-------+------+---------------+------+---------+------+--------+-------------+
| id | select_type | table | type | possible_keys | key  | key_len | ref  | rows   | Extra       |
+----+-------------+-------+------+---------------+------+---------+------+--------+-------------+
|  1 | SIMPLE      | users | ALL  | idx_create_time | NULL | NULL | NULL | 100000 | Using where |
+----+-------------+-------+------+---------------+------+---------+------+--------+-------------+
分析：
- type=ALL：全表扫描！性能差
- rows=100000：扫描10万行
- 原因：DATE() 函数导致索引失效
- 优化：改写为范围查询
  WHERE create_time >= '2024-01-01 00:00:00' 
    AND create_time < '2024-01-02 00:00:00'
*/
```

### 6.2 JOIN 查询

```sql
-- 示例：优化 JOIN
EXPLAIN SELECT u.name, o.order_no 
FROM users u
JOIN orders o ON u.id = o.user_id
WHERE u.status = 1;

/*
优化前（o.user_id 无索引）：
+----+-------------+-------+--------+---------------+------+---------+------+--------+----------------------------------------------------+
| id | select_type | table | type   | possible_keys | key  | key_len | ref  | rows   | Extra                                              |
+----+-------------+-------+--------+---------------+------+---------+------+--------+----------------------------------------------------+
|  1 | SIMPLE      | u     | ALL    | PRIMARY       | NULL | NULL    | NULL | 10000  | Using where                                        |
|  1 | SIMPLE      | o     | ALL    | NULL          | NULL | NULL    | NULL | 100000 | Using where; Using join buffer (Block Nested Loop) |
+----+-------------+-------+--------+---------------+------+---------+------+--------+----------------------------------------------------+
问题：
- 两表都是 ALL（全表扫描）
- 使用 join buffer，性能差
*/

-- 优化：给 orders.user_id 加索引
ALTER TABLE orders ADD INDEX idx_user_id(user_id);

/*
优化后：
+----+-------------+-------+--------+---------------+-------------+---------+-------------+------+-------------+
| id | select_type | table | type   | possible_keys | key         | key_len | ref         | rows | Extra       |
+----+-------------+-------+--------+---------------+-------------+---------+-------------+------+-------------+
|  1 | SIMPLE      | u     | ALL    | PRIMARY       | NULL        | NULL    | NULL        | 10000| Using where |
|  1 | SIMPLE      | o     | ref    | idx_user_id   | idx_user_id | 8       | test.u.id   | 10   | NULL        |
+----+-------------+-------+--------+---------------+-------------+---------+-------------+------+-------------+
改进：
- o 表 type 从 ALL 变为 ref
- rows 从 100000 降到 10
- 不再使用 join buffer
*/
```

### 6.3 子查询

```sql
-- IN 子查询
EXPLAIN SELECT * FROM users WHERE id IN (
    SELECT user_id FROM orders WHERE amount > 1000
);

/*
MySQL 5.6+ 优化为 SEMI-JOIN 或物化：
+----+--------------+-------------+--------+---------------+-------------+---------+------+------+-------------+
| id | select_type  | table       | type   | possible_keys | key         | key_len | ref  | rows | Extra       |
+----+--------------+-------------+--------+---------------+-------------+---------+------+------+-------------+
|  1 | SIMPLE       | <subquery2> | ALL    | NULL          | NULL        | NULL    | NULL | ...  | NULL        |
|  1 | SIMPLE       | users       | eq_ref | PRIMARY       | PRIMARY     | 8       | ...  | 1    | NULL        |
|  2 | MATERIALIZED | orders      | range  | idx_amount    | idx_amount  | 8       | NULL | ...  | Using where |
+----+--------------+-------------+--------+---------------+-------------+---------+------+------+-------------+
说明：
- select_type=MATERIALIZED：子查询被物化为临时表
- 物化后可以与外表进行 JOIN 优化
*/
```

---

## 7. 优化案例分析

### 案例 1：隐式类型转换

```sql
-- 表字段：phone VARCHAR(20)，索引 idx_phone
-- 错误写法：
SELECT * FROM users WHERE phone = 13800138000;
-- phone 是字符串，传入整数，发生隐式转换
-- 相当于 CAST(phone AS SIGNED) = 13800138000
-- 索引失效！type=ALL

-- 正确写法：
SELECT * FROM users WHERE phone = '13800138000';
-- type=ref，使用索引
```

### 案例 2：最左前缀法则

```sql
-- 联合索引 idx_name_age(name, age)

-- 错误：跳过 name，只查 age
SELECT * FROM users WHERE age = 20;
-- type=ALL，索引失效

-- 正确：
SELECT * FROM users WHERE name = '张三' AND age = 20;
-- type=ref，使用 idx_name_age

-- 正确（使用前缀）：
SELECT * FROM users WHERE name = '张三';
-- type=ref，使用 idx_name_age（只用了 name 部分）
```

### 案例 3：范围查询优化

```sql
-- 大量数据范围查询可能导致索引失效
SELECT * FROM users WHERE id > 1 AND id < 1000000;
-- 如果匹配行数超过总行数 20-30%，优化器可能选择全表扫描

-- 优化方案：
-- 1. 限制返回数量
SELECT * FROM users WHERE id > 1 LIMIT 1000;

-- 2. 使用覆盖索引
SELECT id FROM users WHERE id > 1;
-- 只需要 id 列，覆盖索引

-- 3. 分页优化
SELECT * FROM users WHERE id > 100000 LIMIT 100;
-- 避免大的 OFFSET
```

### 案例 4：ORDER BY 优化

```sql
-- 表结构：orders(id, user_id, create_time)，索引 idx_user_time(user_id, create_time)

-- 未优化：
SELECT * FROM orders WHERE user_id = 100 ORDER BY create_time DESC LIMIT 10;
-- 如果有大量数据，filesort 会很慢

-- 确保使用索引排序：
EXPLAIN SELECT * FROM orders WHERE user_id = 100 ORDER BY create_time DESC LIMIT 10;
-- Extra: Using where（没有 Using filesort，好！）

-- 错误示例：
SELECT * FROM orders WHERE user_id = 100 ORDER BY create_time DESC, id ASC;
-- 排序方向不一致，会导致 filesort
```

---

## 8. EXPLAIN ANALYZE（MySQL 8.0.18+）

传统 EXPLAIN 只显示 **估算** 的执行计划，而 EXPLAIN ANALYZE 会 **实际执行** 并显示真实耗时。

```sql
EXPLAIN ANALYZE SELECT * FROM users WHERE age > 18;
```

输出示例：

```
-> Filter: (users.age > 18)  (cost=100.45 rows=300) (actual time=0.023..5.234 rows=500 loops=1)
    -> Table scan on users  (cost=100.45 rows=1000) (actual time=0.015..4.123 rows=1000 loops=1)
```

**关键字段：**
- **cost**：估算成本
- **actual time**：实际耗时（首次返回..全部返回）
- **rows**：实际返回行数 vs 估算行数
- **loops**：循环次数

**使用场景：**
- 验证优化器估算是否准确
- 对比优化前后的实际性能
- 定位真正的性能瓶颈

---

## 9. 总结

### EXPLAIN 优化步骤

1. **查看 type**：确保不是 ALL
2. **查看 key**：确认是否使用了索引
3. **查看 key_len**：确认索引利用率
4. **查看 rows**：扫描行数是否过大
5. **查看 Extra**：检查是否有 Using filesort、Using temporary

### 优化口诀

```
type 避免 ALL，
key 不能是 NULL，
extra 不怕 where，
最怕 filesort temporary。
```

### 面试金句

> "EXPLAIN 是 MySQL 性能优化的基础工具。通过分析 type 判断访问类型（避免 ALL），通过 Extra 判断是否使用覆盖索引（Using index）或需要文件排序（Using filesort）。优化时要确保查询能利用索引，避免函数导致索引失效，减少全表扫描和临时表使用。"

---

**参考资料：**
- MySQL 官方文档：https://dev.mysql.com/doc/refman/8.0/en/explain-output.html
- 《高性能 MySQL》第3版
- 《MySQL 技术内幕：SQL 编程》

**适用版本：** MySQL 5.6 / 5.7 / 8.0 / 9.0