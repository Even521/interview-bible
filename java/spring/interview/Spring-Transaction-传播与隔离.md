1. [事务基础概念](#事务基础概念)
2. [事务传播行为（7种）](#事务传播行为7种)
3. [事务隔离级别（4种）](#事务隔离级别4种)
4. [实际场景分析](#实际场景分析)
5. [常见问题与陷阱](#常见问题与陷阱)
6. [最佳实践](#最佳实践)

---

## 事务基础概念

### 什么是事务？

事务（Transaction）是数据库操作的最小工作单元，它包含的一系列操作要么全部成功，要么全部失败回滚。

```sql
-- 简单事务示例
BEGIN TRANSACTION;
UPDATE account SET balance = balance - 100 WHERE id = 1;  -- 扣款
UPDATE account SET balance = balance + 100 WHERE id = 2;  -- 收款
COMMIT;  -- 提交事务（两个操作都成功）
-- 或 ROLLBACK; 回滚事务（任一操作失败）
```

### Spring 事务管理

Spring 提供了声明式事务管理（Declarative Transaction Management），通过 `@Transactional` 注解轻松实现事务控制。

```java
@Service
public class OrderService {
    
    @Transactional
    public void createOrder(Order order) {
        orderDao.save(order);
        inventoryService.deduct(order.getItemId(), order.getQuantity());
        // 任一操作失败，全部回滚
    }
}
```

---

## 事务传播行为（7种）

传播行为定义了：**当前方法被调用时，如何与现有事务进行交互**。

```java
public enum Propagation {
    REQUIRED,       // 默认
    SUPPORTS,
    MANDATORY,
    REQUIRES_NEW,
    NOT_SUPPORTED,
    NEVER,
    NESTED
}
```

### 传播行为对照表

| 传播行为 | 无事务时 | 有事务时 | 使用场景 |
|---------|---------|---------|---------|
| **REQUIRED** | 新建事务 | 加入现有事务 | 默认，大多数情况 |
| **SUPPORTS** | 非事务运行 | 加入现有事务 | 读操作，非必须事务 |
| **MANDATORY** | 抛异常 | 加入现有事务 | 强制必须在事务中 |
| **REQUIRES_NEW** | 新建事务 | 挂起现有，新建独立事务 | 独立事务，如日志记录 |
| **NOT_SUPPORTED** | 非事务运行 | 挂起现有，非事务运行 | 不需要事务，如查询 |
| **NEVER** | 非事务运行 | 抛异常 | 禁止在事务中运行 |
| **NESTED** | 新建事务 | 创建嵌套事务（savepoint）| 父子事务，父失败子回滚 |

### 详细解析

#### 1. REQUIRED（默认）

```java
@Service
public class OrderService {
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void createOrder(Order order) {
        orderDao.save(order);
        paymentService.process(order);  // 加入当前事务
    }
}

@Service
public class PaymentService {
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void process(Order order) {
        // 加入 OrderService 的事务
        // 如果抛异常，整个事务回滚
        paymentDao.save(order.getPayment());
    }
}
```

**图示**：
```
┌─────────────────────────────────────────┐
│           OrderService                  │
│      createOrder() [REQUIRED]            │
│  ┌─────────────────────────────────┐   │
│  │      PaymentService             │   │
│  │   process() [REQUIRED]          │   │
│  │   (加入现有事务)                 │   │
│  └─────────────────────────────────┘   │
│              同一事务                   │
└─────────────────────────────────────────┘
```

#### 2. REQUIRES_NEW（独立事务）

```java
@Service
public class OrderService {
    
    @Autowired
    private LogService logService;
    
    @Transactional
    public void createOrder(Order order) {
        orderDao.save(order);
        
        try {
            // 独立事务，不影响主业务
            logService.recordLog("创建订单: " + order.getId());
        } catch (Exception e) {
            // 日志失败不影响订单创建
        }
    }
}

@Service
public class LogService {
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLog(String message) {
        logDao.save(new Log(message));
        // 如果这里抛异常，只回滚日志，不回滚订单
    }
}
```

**图示**：
```
┌─────────────────────────────────────────┐
│           OrderService                  │
│      createOrder() [REQUIRED]            │
│           Transaction A 开始              │
│  ┌─────────────────────────────────┐   │
│  │      LogService                 │   │
│  │   recordLog() [REQUIRES_NEW]    │   │
│  │   Transaction B 开始             │   │
│  │   Transaction B 提交/回滚         │   │
│  └─────────────────────────────────┘   │
│           Transaction A 继续              │
└─────────────────────────────────────────┘
```

#### 3. NESTED（嵌套事务）

```java
@Service
public class OrderService {
    
    @Autowired
    private PaymentService paymentService;
    
    @Transactional
    public void createOrder(Order order) {
        orderDao.save(order);
        
        try {
            paymentService.process(order);  // 嵌套事务
        } catch (PaymentException e) {
            // 支付失败，只回滚支付，不回滚订单
            // 订单状态改为"待支付"
        }
    }
}

@Service
public class PaymentService {
    
    @Transactional(propagation = Propagation.NESTED)
    public void process(Order order) {
        // 创建 savepoint
        // 如果失败，回滚到 savepoint
        paymentGateway.charge(order);
    }
}
```

**图示**：
```
Transaction A 开始
    ├── 操作 A1 ✅
    ├── 操作 A2 ✅
    ├── SAVEPOINT nested_tx
    │   ├── Nested Transaction B 开始
    │   ├── 操作 B1 ✅
    │   ├── 操作 B2 ❌ (失败)
    │   └── 回滚到 SAVEPOINT ⬅️ 只回滚 B
    ├── 操作 A3 ✅
    └── Transaction A 提交 ⬅️ A1, A2, A3 提交
```

#### 4. MANDATORY（强制要求事务）

```java
@Service
public class OrderService {
    
    // 必须在事务中调用
    @Transactional(propagation = Propagation.MANDATORY)
    public void processOrder(Order order) {
        // 如果没有事务环境直接调用，抛异常
        // IllegalTransactionStateException
    }
}

// 正确调用方式
@Service
public class OrderManager {
    
    @Autowired
    private OrderService orderService;
    
    @Transactional
    public void batchProcess(List<Order> orders) {
        for (Order order : orders) {
            orderService.processOrder(order);  // ✅ 在事务中
        }
    }
}

// 错误调用方式
@Service
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    public void handleRequest(Order order) {
        orderService.processOrder(order);  // ❌ 没有事务，抛异常
    }
}
```

#### 5. SUPPORTS、NOT_SUPPORTED、NEVER

```java
@Service
public class ReportService {
    
    // 支持事务，但不是必须的
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Order> getOrders(Map<String, Object> params) {
        // 调用时有事务则加入，无时非事务运行
        return orderDao.query(params);
    }
    
    // 不需要事务，有事务也挂起
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void generateReport() {
        // 复杂的查询操作，不需要事务
        // 可以提高并发性能
    }
    
    // 禁止在事务中执行
    @Transactional(propagation = Propagation.NEVER)
    public void updateConfig(Config config) {
        // 如果当前有事务，抛异常
    }
}
```

---

## 事务隔离级别（4种）

隔离级别解决了**多个事务并发执行时**的数据一致性问题。

### 三种并发问题

| 问题 | 说明 | 示例 |
|-----|------|-----|
| **脏读** | 读到其他事务未提交的数据 | A修改了数据但未提交，B读到了修改后的值，A回滚 |
| **不可重复读** | 同一事务中两次读取结果不同 | A读数据，B修改并提交，A再次读取 data ≠ data |
| **幻读** | 同一事务中两次查询，结果集行数不同 | A查询有3条，B插入新数据，A再次查询有4条 |

### 四种隔离级别

```java
public enum Isolation {
    READ_UNCOMMITTED,   // 读未提交
    READ_COMMITTED,     // 读已提交（Oracle默认）
    REPEATABLE_READ,    // 可重复读（MySQL默认）
    SERIALIZABLE        // 串行化
}
```

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | 性能 | 数据库默认 |
|---------|-----|-----------|-----|-----|----------|
| READ_UNCOMMITTED | ✓ 允许 | ✓ 允许 | ✓ 允许 | 最高 | - |
| READ_COMMITTED | ✗ 不允许 | ✓ 允许 | ✓ 允许 | 高 | Oracle |
| REPEATABLE_READ | ✗ 不允许 | ✗ 不允许 | ✓ 允许 | 中 | MySQL |
| SERIALIZABLE | ✗ 不允许 | ✗ 不允许 | ✗ 不允许 | 最低 | - |

### 代码配置

```java
@Service
public class TransferService {
    
    // 读已提交，防止脏读
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void queryBalance(Long accountId) {
        // 只读取已提交的数据
    }
    
    // 可重复读，防止脏读和不可重复读
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void processWithConsistency(Long orderId) {
        // 多次读取同一数据结果一致
    }
    
    // 串行化，完全隔离
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void criticalOperation(Long id) {
        // 最高隔离级别，性能最差
    }
}
```

### MySQL 实现机制

| 隔离级别 | 实现机制 | 锁类型 |
|---------|---------|--------|
| READ_UNCOMMITTED | 不加锁/不检查 | 无 |
| READ_COMMITTED | MVCC（每次读生成ReadView）| 行锁 |
| REPEATABLE_READ | MVCC（事务开始生成ReadView）| Gap锁 |
| SERIALIZABLE | 锁所有读取的行 + Gap锁 | 表锁/行锁 |

```sql
-- 查看MySQL隔离级别
SELECT @@transaction_isolation;  -- MySQL 8.0+
SELECT @@tx_isolation;            -- MySQL 5.7

-- 设置隔离级别
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
```

---

## 实际场景分析

### 场景一：电商下单（REQUIRED + REQUIRES_NEW）

```java
@Service
public class OrderCreateService {
    
    @Autowired
    private InventoryService inventoryService;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private LogService logService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Transactional
    public OrderResult createOrder(CreateOrderRequest request) {
        // 1. 保存订单（主事务）
        Order order = saveOrder(request);
        
        // 2. 扣减库存（主事务）
        inventoryService.deduct(order);
        
        // 3. 处理支付（主事务）
        PaymentResult payment = paymentService.process(order);
        
        // 4. 记录日志（独立事务）
        // 无论支付成败，日志都要记录
        logService.recordOrderLog(order);
        
        // 5. 发送通知（独立事务）
        notificationService.send(order);
        
        return new OrderResult(order, payment);
    }
}

@Service
public class LogService {
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOrderLog(Order order) {
        // 独立事务，失败不影响主业务
        logDao.save(new OrderLog(order));
    }
}

@Service
public class NotificationService {
    
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(Order order) {
        // 异步发送通知，独立事务
        smsService.send(order.getUserPhone(), "订单创建成功");
    }
}
```

**事务边界**：
```
createOrder() [REQUIRED - 主事务]
├── saveOrder()           [加入主事务]
├── inventoryService.deduct() [加入主事务]
├── paymentService.process()  [加入主事务]
├── logService.recordOrderLog()
│   └── [REQUIRES_NEW - 独立事务] ✅ 失败不影响主事务
└── notificationService.send()
    └── [REQUIRES_NEW - 独立事务] ✅ 异步执行
```

### 场景二：银行转账（NESTED）

```java
@Service
public class TransferService {
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private LimitService limitService;
    
    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        // 检查转账限额（嵌套事务）
        try {
            limitService.checkLimit(fromId, amount);
        } catch (LimitExceededException e) {
            // 限额检查失败，记录预警但不影响主流程
            log.warn("转账超出限额: {} -> {} 金额: {}", fromId, toId, amount);
        }
        
        // 执行转账（主事务）
        accountService.debit(fromId, amount);
        accountService.credit(toId, amount);
    }
}

@Service
public class LimitService {
    
    @Transactional(propagation = Propagation.NESTED)
    public void checkLimit(Long accountId, BigDecimal amount) {
        BigDecimal dailyLimit = getDailyLimit(accountId);
        BigDecimal used = getTodayUsed(accountId);
        
        if (used.add(amount).compareTo(dailyLimit) > 0) {
            throw new LimitExceededException("超出日限额");
        }
        // 记录限额使用
        recordLimitUsage(accountId, amount);
    }
}
```

### 场景三：库存扣减（REPEATABLE_READ 防超卖）

```java
@Service
public class InventoryService {
    
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public boolean deduct(Long itemId, Integer quantity) {
        // 查询当前库存（加间隙锁）
        Integer stock = inventoryDao.getStock(itemId);
        
        if (stock < quantity) {
            return false; // 库存不足
        }
        
        // 模拟业务处理时间
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 再次确认并扣减（保证可重复读）
        int affected = inventoryDao.decreaseStock(itemId, quantity);
        return affected > 0;
    }
}
```

---

## 常见问题与陷阱

### 问题一：事务失效（同类调用）

```java
@Service
public class OrderService {
    
    @Transactional
    public void createOrder(Order order) {
        // ... 业务逻辑
        updateInventory(order);  // ❌ 直接调用，事务不生效！
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateInventory(Order order) {
        // 期望新事务，实际是同一个事务
        inventoryDao.deduct(order);
    }
}
```

**解决方案**：

```java
@Service
public class OrderService {
    
    @Autowired
    private OrderService self; // 注入自身代理
    
    @Transactional
    public void createOrder(Order order) {
        // ... 业务逻辑
        self.updateInventory(order);  // ✅ 通过代理调用
        
        // 或拆分到另一个类
        inventoryService.update(order);
    }
}
```

### 问题二：异常被吞没

```java
@Service
public class OrderService {
    
    @Transactional
    public void createOrder(Order order) {
        try {
            paymentService.process(order);
        } catch (Exception e) {
            // ❌ 吞没异常，事务不会回滚！
            log.error("支付失败", e);
            return;
        }
    }
}
```

**解决方案**：

```java
@Transactional
public void createOrder(Order order) {
    try {
        paymentService.process(order);
    } catch (Exception e) {
        log.error("支付失败", e);
        // ✅ 主动回滚或抛出运行时异常
        throw new RuntimeException("支付失败", e);
        
        // 或手动标记回滚
        // TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    }
}
```

### 问题三：非运行时异常不回滚

```java
@Service
public class OrderService {
    
    // 默认只回滚 RuntimeException 和 Error
    @Transactional
    public void createOrder(Order order) throws Exception {
        // 抛 checked Exception，事务不会回滚！
        throw new IOException("网络异常");
    }
}
```

**解决方案**：

```java
// 指定需要回滚的异常
@Transactional(rollbackFor = Exception.class) // 回滚所有异常
public void createOrder(Order order) throws Exception {
    // ...
}

// 或指定不回滚的异常
@Transactional(noRollbackFor = BusinessException.class)
public void createOrder(Order order) {
    // BusinessException 不触发回滚
}
```

### 问题四：多线程下事务失效

```java
@Service
public class OrderService {
    
    @Autowired
    private ExecutorService executor;
    
    @Transactional
    public void batchCreate(List<Order> orders) {
        for (Order order : orders) {
            // ❌ 新线程中的操作不在事务中！
            executor.submit(() -> createSingle(order));
        }
    }
    
    @Transactional
    public void createSingle(Order order) {
        orderDao.save(order);
    }
}
```

**原因**：事务上下文绑定在线程的 ThreadLocal 中，子线程无法获取。

**解决方案**：

```java
@Service
public class OrderService {
    
    @Transactional
    public void batchCreate(List<Order> orders) {
        for (Order order : orders) {
            createSingle(order); // ✅ 单线程执行
        }
    }
    
    // 或实现分布式事务（Seata、Saga等）
}
```

### 问题五：读写分离与事务传播

```java
@Service
public class OrderQueryService {
    
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Order getOrder(Long id) {
        // 支持事务，利用只读优化
        return orderDao.findById(id);
    }
    
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<Order> listOrders() {
        // 完全非事务，可能走从库
        return orderDao.findAll();
    }
}
```

---

## 最佳实践

### 1. 传播行为选择指南

```java
// 默认值，适用大多数场景
@Transactional(propagation = Propagation.REQUIRED)

// 独立事务场景
@Transactional(propagation = Propagation.REQUIRES_NEW)
// - 日志记录
// - 审计追踪
// - 异步操作记录

// 嵌套事务场景
@Transactional(propagation = Propagation.NESTED)
// - 可选业务步骤
// - 失败回滚点

// 查询场景
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
```

### 2. 隔离级别选择

| 场景 | 推荐隔离级别 | 说明 |
|-----|------------|-----|
| 普通查询 | READ_COMMITTED | 避免脏读即可 |
| 财务报表 | REPEATABLE_READ | 统计需要一致性 |
| 库存扣减 | REPEATABLE_READ + 乐观锁 | 防止超卖 |
| 关键账户 | SERIALIZABLE | 最高安全要求 |

### 3. 完整配置示例

```java
@Service
public class BestPracticeService {
    
    /**
     * 通用业务方法
     */
    @Transactional(
        propagation = Propagation.REQUIRED,     // 默认传播行为
        isolation = Isolation.READ_COMMITTED,   // 读已提交
        timeout = 30,                           // 30秒超时
        readOnly = false,                       // 可读写
        rollbackFor = {Exception.class}        // 所有异常都回滚
    )
    public void businessMethod(BusinessParam param) {
        // 业务逻辑
    }
    
    /**
     * 查询方法
     */
    @Transactional(
        propagation = Propagation.SUPPORTS,
        isolation = Isolation.READ_COMMITTED,
        readOnly = true  // 优化查询
    )
    public List<Result> queryMethod(QueryParam param) {
        // 查询逻辑
        return results;
    }
    
    /**
     * 独立事务方法
     */
    @Transactional(
        propagation = Propagation.REQUIRES_NEW, // 独立事务
        isolation = Isolation.READ_COMMITTED
    )
    public void independentMethod(Data data) {
        // 必须成功记录的操作
    }
}
```

### 4. 事务超时与并发控制

```java
@Service
public class ConcurrentService {
    
    // 长事务需要设置超时
    @Transactional(timeout = 60) // 60秒
    public void longRunningProcess() {
        // 批量处理
    }
    
    // 乐观锁防止并发冲突
    @Transactional
    public void updateWithVersion(Data data) {
        int rows = dao.updateWithVersion(data.getId(), data.getVersion(), data);
        if (rows == 0) {
            throw new ConcurrentUpdateException("数据已被修改，请重试");
        }
    }
    
    // 悲观锁
    @Transactional
    public void updateWithLock(Long id) {
        Data data = dao.selectForUpdate(id); // SELECT ... FOR UPDATE
        // 处理数据
        dao.update(data);
    }
}
```

### 5. 命名规范建议

```java
// ✅ 推荐：查用 supports/rule，改用 required
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public Order getOrder(Long id);

@Transactional(propagation = Propagation.REQUIRED)
public void saveOrder(Order order);

// ✅ 推荐：提取公共配置
public @interface BusinessTransaction {
    @AliasFor(annotation = Transactional.class)
    String value() default "";
}

@BusinessTransaction
public void businessMethod();
```

---

## 总结

### 快速记忆

**传播行为**：
- `REQUIRED`：有就加入，没有新建（最常用）
- `REQUIRES_NEW`：各干各的（日志、通知）
- `NESTED`：父子关系（savepoint）
- `MANDATORY`：必须有（强制约束）

**隔离级别**：
```
脏读 ← READ_UNCOMMITTED ← 性能最高
  ↓
不可重复读 ← READ_COMMITTED ← Oracle默认
  ↓
幻读 ← REPEATABLE_READ ← MySQL默认
  ↓
无问题 ← SERIALIZABLE ← 性能最低
```

### 面试金句

> "Spring 提供了 7 种事务传播行为，最常用的是 REQUIRED（默认）和 REQUIRES_NEW。隔离级别有 4 种，MySQL 默认 REPEATABLE_READ，Oracle 默认 READ_COMMITTED。实际开发中要注意事务失效的场景：同类调用、异常被吞默、非运行时异常默认不回滚等。"

---

**参考文档**：
- Spring 官方文档：https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction
- MySQL 事务隔离：https://dev.mysql.com/doc/refman/8.0/en/innodb-transaction-isolation-levels.html