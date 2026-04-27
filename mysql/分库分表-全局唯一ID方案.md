
### 全局唯一 ID 的要求

| 要求 | 说明 |
|------|------|
| **全局唯一** | 分布式环境下不重复 |
| **趋势递增** | 方便 B+ 树索引插入 |
| **信息安全** | 不泄露业务数据量 |
| **高可用** | 生成服务不能单点故障 |
| **高性能** | 高并发下快速生成 |

---

## 2. 常见方案对比

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|---------|
| **UUID** | 简单、本地生成 | 无序、存储空间大 | 日志、消息等 |
| **数据库自增** | 简单、有序 | 单点瓶颈、不适合分库 | 小数据量 |
| **号段模式** | 高性能、趋势递增 | 依赖数据库 | 高并发业务 |
| **雪花算法** | 本地生成、高性能 | 依赖时钟 | 多数业务场景 |
| **Leaf** | 高可用、可扩展 | 部署复杂 | 大规模业务 |

---

## 3. 雪花算法（Snowflake）

### 3.1 算法原理

Twitter 开源的分布式 ID 生成算法，生成 64 位长整型 ID。

``` 
┌─────────────────────────────────────────────────────────────────┐
│                      64 位 Snowflake ID                         │
├──────────┬───────────────┬────────────────┬───────────────────┤
│  1 bit   │    41 bit     │    10 bit      │     12 bit        │
│  符号位  │   时间戳      │   工作机器 ID   │     序列号        │
│ （保留） │  (毫秒级)     │ (数据中心+机器) │  (每毫秒自增)     │
└──────────┴───────────────┴────────────────┴───────────────────┘
        │              │                  │
        ▼              ▼                  ▼
     始终为0      69年可用时间         最多1024个节点    
                                   每节点每毫秒4096个ID
```

### 3.2 代码实现

```java
/**
 * Twitter Snowflake 算法实现
 */
public class SnowflakeIdGenerator {
    
    // 起始时间戳 (2024-01-01)
    private final long twepoch = 1704067200000L;
    
    // 位数分配
    private final long dataCenterIdBits = 5L;  // 数据中心占5位
    private final long workerIdBits = 5L;      // 机器ID占5位
    private final long sequenceBits = 12L;     // 序列号占12位
    
    // 最大值
    private final long maxDataCenterId = ~(-1L << dataCenterIdBits);
    private final long maxWorkerId = ~(-1L << workerIdBits);
    private final long maxSequence = ~(-1L << sequenceBits);
    
    // 位移
    private final long workerIdShift = sequenceBits;
    private final long dataCenterIdShift = sequenceBits + workerIdBits;
    private final long timestampShift = sequenceBits + workerIdBits + dataCenterIdBits;
    
    private long dataCenterId;  // 数据中心ID
    private long workerId;      // 机器ID
    private long sequence = 0L; // 序列号
    private long lastTimestamp = -1L; // 上次生成ID的时间戳
    
    public SnowflakeIdGenerator(long dataCenterId, long workerId) {
        if (dataCenterId > maxDataCenterId || dataCenterId < 0) {
            throw new IllegalArgumentException("DataCenter ID out of range");
        }
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException("Worker ID out of range");
        }
        this.dataCenterId = dataCenterId;
        this.workerId = workerId;
    }
    
    /**
     * 生成下一个ID
     */
    public synchronized long nextId() {
        long timestamp = timeGen();
        
        // 时钟回退检查
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards!");
        }
        
        // 同一毫秒内
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & maxSequence;
            // 序列号溢出，等待下一毫秒
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒，序列号重置
            sequence = 0L;
        }
        
        lastTimestamp = timestamp;
        
        // 组合ID
        return ((timestamp - twepoch) << timestampShift)
                | (dataCenterId << dataCenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }
    
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }
    
    private long timeGen() {
        return System.currentTimeMillis();
    }
    
    public static void main(String[] args) {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        
        // 批量生成ID
        for (int i = 0; i < 10; i++) {
            long id = generator.nextId();
            System.out.println("ID: " + id + " | Binary: " + Long.toBinaryString(id));
        }
    }
}
```

### 3.3 雪花算法优缺点

**优点：**
- ✅ 本地生成，无需网络开销
- ✅ 趋势递增，插入性能好
- ✅ 高性能，单机每秒可生成数百万 ID
- ✅ 包含时间信息，可反解

**缺点：**
- ❌ **时钟回拨问题**：依赖系统时间，时钟回拨会导致 ID 重复
- ❌ 需要分配 dataCenterId 和 workerId

### 3.4 时钟回拨解决方案

```java
/**
 * 改进版雪花算法 - 解决时钟回拨
 */
public class SafeSnowflakeIdGenerator {
    private final long twepoch = 1704067200000L;
    private final long workerIdBits = 10L;
    private final long sequenceBits = 12L;
    private final long maxWorkerId = ~(-1L << workerIdBits);
    private final long workerIdShift = sequenceBits;
    private final long timestampShift = sequenceBits + workerIdBits;
    
    private final long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    
    // 最大允许时钟回拨毫秒数
    private final long maxBackwardMillis = 5;
    
    public SafeSnowflakeIdGenerator(long workerId) {
        this.workerId = workerId;
    }
    
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        
        // 时钟回拨检测
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= maxBackwardMillis) {
                // 小范围回拨，等待追赶
                try {
                    Thread.sleep(offset + 1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                timestamp = System.currentTimeMillis();
            } else {
                // 大范围回拨，抛异常或等待
                throw new RuntimeException(
                    "Clock moved backwards too far: " + offset + "ms"
                );
            }
        }
        
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & 4095;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        
        lastTimestamp = timestamp;
        
        return ((timestamp - twepoch) << timestampShift)
                | (workerId << workerIdShift)
                | sequence;
    }
    
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
```

---

## 4. 美团 Leaf 方案

### 4.1 Leaf 架构概述

美团开源的分布式 ID 生成服务，提供两种模式：号段模式（Leaf-segment）和雪花模式（Leaf-snowflake）。

``` 
┌─────────────────────────────────────────────┐
│              Leaf Server 集群                  │
├──────────────┬──────────────────────────────┤
│ Leaf-segment │      Leaf-snowflake           │
│   号段模式    │         雪花模式               │
├──────────────┼──────────────────────────────┤
│ 基于数据库     │      基于 Zookeeper           │
│ 批量获取号段   │      分配 Worker ID           │
└──────────────┴──────────────────────────────┘
```

### 4.2 Leaf-segment 号段模式

**原理**：从数据库批量获取一段 ID，本地内存分配。

``` 
数据库表设计：
┌─────────────┬─────────────┬─────────────┬──────────────┐
│    biz_tag  │  max_id     │   step      │  update_time │
├─────────────┼─────────────┼─────────────┼──────────────┤
│ order_id    │   100000    │   1000      │  2024-01-01  │
│ user_id     │   500000    │   1000      │  2024-01-01  │
└─────────────┴─────────────┴─────────────┴──────────────┘

获取号段：
1. UPDATE leaf_alloc SET max_id = max_id + step WHERE biz_tag = 'order_id'
2. 返回旧 max_id ~ 新 max_id（如 100000 ~ 101000）
3. 本地内存分配 100000, 100001, ..., 100999
4. 用完后再去数据库获取下一号段
```

**Java 实现：**

```java
/**
 * Leaf 号段模式实现
 */
public class LeafSegmentIdGenerator {
    
    private final DataSource dataSource;
    private final String bizTag;
    private final int step;
    
    // 当前号段
    private volatile long currentId;
    private volatile long maxId;
    private volatile long loadingMaxId;
    
    // 双 Buffer
    private final Segment[] segments = new Segment[2];
    private volatile int currentSegmentIndex = 0;
    
    public LeafSegmentIdGenerator(DataSource ds, String bizTag) {
        this.dataSource = ds;
        this.bizTag = bizTag;
        this.step = 1000; // 默认步长
        
        // 初始化两个号段
        segments[0] = new Segment();
        segments[1] = new Segment();
        
        // 加载第一个号段
        loadNextSegment(segments[0]);
    }
    
    /**
     * 获取下一个ID
     */
    public long nextId() {
        Segment segment = segments[currentSegmentIndex];
        
        // 当前号段用完，切换到下一个
        if (!segment.nextId()) {
            switchToNextSegment();
            segment = segments[currentSegmentIndex];
        }
        
        // 异步加载下一个号段（如果当前号段已使用50%）
        if (segment.getIdle() < step * 0.5 
            && !segments[(currentSegmentIndex + 1) % 2].isReady()) {
            loadNextSegmentAsync(segments[(currentSegmentIndex + 1) % 2]);
        }
        
        return segment.getAndIncrement();
    }
    
    /**
     * 从数据库加载号段
     */
    private synchronized void loadNextSegment(Segment segment) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            
            // 使用悲观锁或乐观更新
            String sql = "UPDATE leaf_alloc " +
                        "SET max_id = max_id + ?, update_time = NOW() " +
                        "WHERE biz_tag = ?";
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, step);
                ps.setString(2, bizTag);
                ps.executeUpdate();
            }
            
            // 获取新的 max_id
            String selectSql = "SELECT max_id FROM leaf_alloc WHERE biz_tag = ?";
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, bizTag);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    long newMaxId = rs.getLong("max_id");
                    segment.init(newMaxId - step, newMaxId);
                }
            }
            
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Load segment failed", e);
        }
    }
    
    /**
     * 号段对象
     */
    private static class Segment {
        private volatile long currentId;
        private volatile long maxId;
        private volatile boolean ready = false;
        
        public synchronized void init(long minId, long maxId) {
            this.currentId = minId;
            this.maxId = maxId;
            this.ready = true;
        }
        
        public boolean nextId() {
            return currentId < maxId;
        }
        
        public synchronized long getAndIncrement() {
            return currentId++;
        }
        
        public long getIdle() {
            return maxId - currentId;
        }
        
        public boolean isReady() {
            return ready;
        }
    }
}
```

### 4.3 Leaf-snowflake 雪花模式

**原理**：使用 Zookeeper 或 Consul 管理 Worker ID，解决雪花算法的配置问题。

``` 
┌─────────────────────────────────────────┐
│           Zookeeper 集群                 │
│    ┌─────────────────────────────┐       │
│    │  /leaf/server-1/worker-id  │       │
│    │  /leaf/server-2/worker-id  │       │
│    │  /leaf/server-3/worker-id  │       │
│    └─────────────────────────────┘       │
│                                         │
│  1. 服务启动时注册到 Zookeeper            │
│  2. 获取唯一的 worker-id                │
│  3. 定期心跳保活                        │
│  4. 服务下线时释放 worker-id            │
└─────────────────────────────────────────┘
```

**配置示例：**

```properties
# Leaf 配置
leaf.name=com.sankuai.leaf.opensource.test
leaf.segment.enable=true
leaf.segment.url=jdbc:mysql://localhost:3306/leaf
leaf.segment.bisiness=order_id,user_id

leaf.snowflake.enable=true
leaf.snowflake.address=zookeeper://localhost:2181
leaf.snowflake.port=8081
```

### 4.4 Leaf 优缺点

**优点：**
- ✅ 高可用：支持集群部署
- ✅ 高性能：号段模式本地分配，极快
- ✅ 易扩展：可动态增加节点
- ✅ 无需配置：自动分配 Worker ID

**缺点：**
- ❌ 依赖中间件（数据库/Zookeeper）
- ❌ 号段模式 ID 不是严格连续
- ❌ 部署运维成本较高

---

## 5. 其他方案

### 5.1 UUID

```java
// Java UUID 生成
String uuid = UUID.randomUUID().toString();
// 结果：550e8400-e29b-41d4-a716-446655440000

// 去掉横线
String uuid2 = UUID.randomUUID().toString().replace("-", "");
```

**优缺点：**
- ✅ 简单、本地生成
- ❌ 无序、存储空间大（36字符）
- ❌ 索引性能差（B+树频繁分裂）

**改进：有序 UUID**
```java
// 基于时间的有序 UUID
public static UUID orderedUUID() {
    long time = System.currentTimeMillis();
    return new UUID(time, UUID.randomUUID().getLeastSignificantBits());
}
```

### 5.2 Redis 自增

```java
/**
 * Redis 分布式 ID 生成
 */
@Component
public class RedisIdGenerator {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    /**
     * 生成自增 ID
     */
    public long nextId(String key) {
        return redisTemplate.opsForValue().increment("id:" + key);
    }
    
    /**
     * 批量获取（减少网络往返）
     */
    public List<Long> nextIds(String key, int count) {
        long start = redisTemplate.opsForValue()
            .increment("id:" + key, count);
        List<Long> ids = new ArrayList<>();
        for (long i = start - count + 1; i <= start; i++) {
            ids.add(i);
        }
        return ids;
    }
}
```

**优缺点：**
- ✅ 简单、性能高
- ✅ 支持原子操作
- ❌ 依赖 Redis（需要高可用）
- ❌ 需要持久化机制

### 5.3 数据库号段

```java
/**
 * 数据库号段模式（简化版）
 */
public class DatabaseSegmentIdGenerator {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final AtomicLong currentId = new AtomicLong(0);
    private volatile long maxId = 0;
    private final int step = 1000;
    
    public synchronized long nextId() {
        if (currentId.get() >= maxId) {
            loadNextSegment();
        }
        return currentId.getAndIncrement();
    }
    
    private void loadNextSegment() {
        // 使用数据库乐观锁或悲观锁
        jdbcTemplate.update(
            "UPDATE sequence SET value = value + ? WHERE name = ?", 
            step, "order_seq"
        );
        
        Long newMax = jdbcTemplate.queryForObject(
            "SELECT value FROM sequence WHERE name = ?", 
            Long.class, "order_seq"
        );
        
        this.maxId = newMax;
        this.currentId.set(newMax - step);
    }
}
```

---

## 6. 方案选型建议

### 6.1 选型决策树

``` 
                    ┌───────────────────┐
                    │   ID 生成需求场景  │
                    └─────────┬─────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
        ┌──────────┐     ┌──────────┐     ┌──────────┐
        │ 小数据量  │     │ 中等规模  │     │ 大规模    │
        │ <1万/秒  │     │ 1-10万/秒 │     │ >10万/秒 │
        └────┬─────┘     └────┬─────┘     └────┬─────┘
             │                │                │
             ▼                ▼                ▼
        ┌──────────┐     ┌──────────┐     ┌──────────┐
        │ 数据库    │     │ 雪花算法  │     │ Leaf     │
        │ 自增     │     │ +时钟回拨 │     │ 号段模式  │
        │ Redis   │     │ 保护     │     │ +高可用   │
        └──────────┘     └────────━━┘     └──────────┘
```

### 6.2 不同业务场景推荐

| 场景 | 推荐方案 | 说明 |
|------|---------|------|
| **订单 ID** | 雪花算法 / Leaf | 趋势递增，支持高并发 |
| **用户 ID** | Leaf 号段模式 | 严格递增，可读性好 |
| **消息 ID** | 雪花算法 | 包含时间信息 |
| **日志 ID** | UUID / 雪花 | 无严格要求 |
| **分片键** | 雪花算法 | 支持数据均匀分布 |

### 6.3 性能对比

| 方案 | 单机 QPS | 可用性 | 延迟 |
|------|---------|-------|------|
| UUID | 10万+ | 99.99% | 极低 |
| 雪花算法 | 409万/节点 | 99.99% | 极低 |
| Leaf-segment | 1000万+ | 99.99% | 极低 |
| Redis | 10万+ | 依赖 Redis | 低 |
| 数据库自增 | 几千 | 依赖 DB | 高 |

---

## 7. 常见问题

### Q1：雪花算法时钟回拨了怎么办？

**方案一：等待追赶（小范围回拨）**
```java
if (offset <= 5) { // 5毫秒内
    Thread.sleep(offset + 1);
}
```

**方案二：使用历史序列号（大范围回拨）**
```java
// 记录最近几毫秒的序列号
// 回拨时使用之前未用完的序列号
```

**方案三：抛出异常（推荐）**
```java
if (offset > maxBackwardMillis) {
    throw new ClockMovedBackwardsException(offset);
}
// 依赖监控和告警，人工介入处理
```

### Q2：Leaf 号段模式 ID 不连续怎么办？

``` 
正常现象：
- 号段用完切换时，ID 会有跳跃（如 1000 -> 2000）
- 服务重启时，未用完的号段会浪费

解决方案（如需连续）：
1. 使用号段模式时不考虑连续性
2. 或切换到雪花算法（趋势递增但不连续）
3. 或设计业务允许不连续（推荐）

注意：严格连续和高性能是互斥的！
```

### Q3：如何生成有业务含义的 ID？

```java
/**
 * 带业务含义的 ID 生成
 * 格式：标识位(1) + 时间戳(41) + 业务类型(4) + 机器ID(6) + 序列号(12)
 */
public class BusinessIdGenerator {
    
    public long generateId(int businessType, long sequence) {
        long timestamp = System.currentTimeMillis() - twepoch;
        
        return (1L << 63)                              // 正数标识
             | (timestamp << 22)                       // 时间戳
             | ((businessType & 0xF) << 18)          // 业务类型
             | ((workerId & 0x3F) << 12)             // 机器ID
             | (sequence & 0xFFF);                     // 序列号
    }
}

// 业务类型定义
public interface BusinessType {
    int ORDER = 1;      // 订单
    int PAYMENT = 2;    // 支付
    int USER = 3;       // 用户
    int PRODUCT = 4;    // 商品
}
```

### Q4：分库分表后如何路由？

```java
/**
 * 根据 ID 路由到对应的库和表
 */
public class ShardingRouter {
    
    /**
     * 获取分库索引（4个库）
     */
    public int getDbIndex(long id) {
        return (int) (id % 4);
    }
    
    /**
     * 获取分表索引（每个库8张表）
     */
    public int getTableIndex(long id) {
        return (int) ((id / 4) % 8);
    }
    
    /**
     * 雪花算法 ID 路由（利用机器ID）
     */
    public int routeByWorkerId(long snowflakeId) {
        // 提取 Worker ID（中间10位）
        long workerId = (snowflakeId >> 12) & 0x3FF;
        return (int) (workerId % 32); // 32张表
    }
}
```

### Q5：分布式 ID 生成服务宕机了怎么办？

``` 
高可用方案：

┌─────────────────────────────────────────┐
│           Leaf 集群（3节点）              │
│    ┌─────────┬─────────┬─────────┐     │
│    │ Node 1  │ Node 2  │ Node 3  │     │
│    │ :8081   │ :8082   │ :8083   │     │
│    └────┬────┴────┬────┴────┬────┘     │
│         │         │         │           │
│    ┌────┴─────────┴─────────┴────┐      │
│    │   Nginx / SLB 负载均衡       │      │
│    │   健康检查 + 故障转移          │      │
│    └─────────────┬─────────────────┘      │
│                  │                       │
│                  ▼                       │
│           客户端 SDK                      │
│    ┌─────────────────────────┐            │
│    │ 1. 本地缓存预加载号段     │            │
│    │ 2. 故障时切换节点        │            │
│    │ 3. 降级使用雪花算法      │            │
│    └─────────────────────────┘            │
└─────────────────────────────────────────┘

降级策略：
1. Leaf 不可用时，退化为本地雪花算法
2. 等待 Leaf 恢复后切换回来
3. 保证服务可用性优先
```

---

## 8. 总结

### 方案选择总结

| 方案 | 推荐指数 | 适用场景 |
|------|---------|---------|
| **雪花算法** | ⭐⭐⭐⭐⭐ | 大多数业务场景 |
| **Leaf-segment** | ⭐⭐⭐⭐⭐ | 超大规模、高并发 |
| **UUID** | ⭐⭐ | 非核心、日志类 |
| **Redis** | ⭐⭐⭐ | 已有 Redis 集群 |

### 关键点记忆

``` 
分布式 ID 生成核心要点：

┌─────────────────────────────────────────┐
│ 1. 趋势递增：雪花算法、号段模式         │
│ 2. 高可用：多节点部署 + 降级策略         │
│ 3. 时钟问题：监控 + 保护机制             │
│ 4. 路由设计：利用 ID 特征做分片          │
│ 5. 业务含义：可嵌入业务标识（可选）       │
└─────────────────────────────────────────┘
```

### 面试金句

> "分库分表场景下，我通常推荐使用雪花算法或美团 Leaf 方案。雪花算法简单易用，本地生成高性能，但需要注意时钟回拨问题；Leaf 号段模式性能更高，适合超大规模场景，但需要依赖数据库做号段分配。选择时要根据业务规模、运维成本综合考虑。"

---

**参考资料：**
- 美团 Leaf 开源：https://github.com/Meituan-Dianping/Leaf
- Twitter Snowflake 论文
- 《百亿级流量系统高可用架构设计》

**适用版本：** 所有 MySQL 版本