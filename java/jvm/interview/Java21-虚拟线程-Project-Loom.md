# Java 21 虚拟线程（Project Loom）详解

> 本文档全面讲解 Java 21 虚拟线程（Virtual Threads）的特性、应用场景，以及与传统线程池的对比分析。

---

## 目录

1. [虚拟线程概述](#1-虚拟线程概述)
2. [核心概念](#2-核心概念)
3. [创建与使用](#3-创建与使用)
4. [应用场景](#4-应用场景)
5. [与传统线程池对比](#5-与传统线程池对比)
6. [最佳实践](#6-最佳实践)
7. [常见问题](#7-常见问题)

---

## 1. 虚拟线程概述

### 1.1 什么是虚拟线程？

虚拟线程（Virtual Threads）是 JDK 21 正式发布的轻量级线程实现，是 Project Loom 的核心特性。

```java
// 传统线程（Platform Thread）- 1:1 映射 OS 线程
Thread.startVirtualThread(() -> {
    System.out.println("Hello from Virtual Thread!");
});
```

**核心特性：**

| 特性 | 说明 |
|------|------|
| **轻量级** | 内存占用仅几百字节（vs 传统线程的 1MB+）|
| **高并发** | 单 JVM 可创建数百万个虚拟线程 |
| **自动调度** | JVM 调度，不直接绑定 OS 线程 |
| **阻塞友好** | 虚拟线程阻塞时自动释放载体线程（Carrier Thread）|
| **兼容性** | 与现有的 Thread API 完全兼容 |

### 1.2 为什么需要虚拟线程？

```java
// 传统线程池的问题：
// - 10000 个并发请求 = 10000 个线程
// - 每个线程占用 ~1MB 内存
// - 总内存占用 ~10GB
// - 大量线程切换开销

// 虚拟线程的优势：
// - 10000 个虚拟线程 = ~10MB 内存
// - 实际只使用少量 OS 线程（如 CPU 核心数）
// - 阻塞时自动挂起，不占用 OS 线程
```

**传统线程的问题：**

```
┌────────────────────────────────────────────┐
│           传统线程（Platform Thread）         │
├────────────────────────────────────────────┤
│                                            │
│  JVM 线程 ←─── 1:1 ───→ OS 线程            │
│     │                      │               │
│     ▼                      ▼               │
│  内存占用 1MB+          系统调度成本高        │
│  创建开销大             上下文切换开销高       │
│  数量有限 (~10000)     阻塞时浪费资源         │
│                                            │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│           虚拟线程（Virtual Thread）        │
├────────────────────────────────────────────┤
│                                            │
│  虚拟线程 ──M:N──→ Platform Thread ── 少量 ──→ OS 线程 │
│                      (载体线程)             │
│     │                                            │
│     ▼                                            │
│  内存占用 ~500 bytes                            │
│  创建极快（约 1μs）                             │
│  数量无限制（百万级）                           │
│  阻塞时自动让出载体线程                         │
│                                            │
└────────────────────────────────────────────┘
```

---

## 2. 核心概念

### 2.1 架构模型

```java
// 虚拟线程架构
Virtual Thread (VT) → JVM 调度 → 绑定到 Platform Thread → OS 线程

// VT 阻塞时
VT 挂起 → 让出 Platform Thread → 其他 VT 复用 Platform Thread

// VT 恢复时
从阻塞返回 → 重新绑定任意可用的 Platform Thread
```

```
┌──────────────────────────────────────────────────────┐
│                    JVM                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐           │
│  │ Virtual  │  │ Virtual  │  │ Virtual  │  ...      │
│  │ Thread 1 │  │ Thread 2 │  │ Thread N │  (100万+) │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘           │
│       │             │             │                  │
│       └─────────────┼─────────────┘                  │
│                     │  ForkJoinPool 调度             │
│              ┌──────┴──────┐                       │
│              │             │                        │
│     ┌────────┴──┐   ┌──────┴───┐  ... 少量         │
│     │ Platform  │   │ Platform │  (CPU 核心数)      │
│     │ Thread 1  │   │ Thread 2 │                    │
│     └────┬──────┘   └─────┬────┘                   │
│          │                │                          │
│          └────────────────┘                          │
│              OS 线程 (4-8个)                         │
└──────────────────────────────────────────────────────┘
```

### 2.2 阻塞操作的处理

```java
// 传统线程阻塞
Thread (Platform)             阻塞期间占用 OS 线程
├── 读取数据库 → 等待 → 继续  (OS 线程被占用)
├── 调用 HTTP API → 等待    (OS 线程被占用)
└── 读写文件 → 等待         (OS 线程被占用)

// 虚拟线程阻塞
Virtual Thread                阻塞时自动挂起
├── 读取数据库 → [挂起] → 释放 Platform Thread
├── HTTP 调用 → [挂起] → 其他虚拟线程复用
└── 文件IO → [挂起] → 恢复时再绑定
```

**关键区别：**

| 场景 | 传统线程 | 虚拟线程 |
|------|---------|---------|
| 网络请求 | 占用 OS 线程等待 | 挂起，释放线程 |
| 数据库查询 | 占用 OS 线程等待 | 挂起，异步恢复 |
| Thread.sleep() | 占用 OS 线程 | 挂起，定时器恢复 |
| synchronized | **会占用** OS 线程（不卸载）| **会占用** OS 线程（注意！）|

---

## 3. 创建与使用

### 3.1 创建虚拟线程

```java
// Java 21 创建虚拟线程的 4 种方式

public class VirtualThreadDemo {
    
    // 方式1：Thread.startVirtualThread()
    public void method1() {
        Thread.startVirtualThread(() -> {
            System.out.println("Virtual Thread: " + 
                Thread.currentThread());
        });
    }
    
    // 方式2：Thread.ofVirtual()
    public void method2() {
        Thread vt = Thread.ofVirtual()
            .name("my-vt-")
            .unstarted(() -> {
                System.out.println("Running");
            });
        vt.start();
    }
    
    // 方式3：Thread.Builder
    public void method3() {
        Thread.Builder builder = Thread.ofVirtual()
            .name("user-thread", 0); // 命名模式
            
        for (int i = 0; i < 5; i++) {
            Thread vt = builder.unstarted(() -> {
                // 任务逻辑
            });
            vt.start();
        }
    }
    
    // 方式4：Executors.newVirtualThreadPerTaskExecutor()
    public void method4() {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 10000; i++) {
                executor.submit(() -> {
                    // 任务逻辑
                    Thread.sleep(Duration.ofSeconds(1));
                    return "done";
                });
            }
        } // 自动关闭
    }
}
```

### 3.2 与传统 Thread API 兼容性

```java
// 完全兼容现有 Thread API
Thread vt = Thread.startVirtualThread(() -> {
    // 代码无需修改即可运行在虚拟线程上
    
    // 所有 Thread 方法都可用
    System.out.println(Thread.currentThread().isVirtual()); // true
    System.out.println(Thread.currentThread().threadId());
    
    // 线程本地变量
    ThreadLocal<String> tl = ThreadLocal.withInitial(() -> "initial");
    tl.set("value");
    System.out.println(tl.get());
    
    // FutureTask、CompletableFuture 等完全兼容
});

// 等待虚拟线程完成
vt.join();
```

### 3.3 Scoped Values（线程本地变量改进）

```java
// Java 21 引入 ScopedValue，替代 ThreadLocal
// ScopedValue 对虚拟线程更友好（不可变，避免内存泄漏）

public class ScopedValueDemo {
    // 定义 Scoped Value
    public static final ScopedValue<String> USER = ScopedValue.newInstance();
    
    public void handleRequest() {
        // 绑定值并在作用域内执行
        ScopedValue.where(USER, "user-123")
            .run(() -> {
                // 在虚拟线程中读取
                System.out.println(USER.get()); // user-123
                
                // 子任务继承 Scoped Value
                Thread.startVirtualThread(() -> {
                    System.out.println(USER.get()); // user-123
                });
            });
        // 作用域结束自动清理
    }
}
```

---

## 4. 应用场景

### 4.1 高并发 Web 服务

```java
// 使用虚拟线程处理 HTTP 请求
// Spring Boot 3.2+ 支持虚拟线程

@SpringBootApplication
public class WebApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }
}
```

```yaml
# application.yml
spring:
  threads:
    virtual:
      enabled: true  # 开启虚拟线程支持
```

```java
// Controller 自动运行在虚拟线程上
@RestController
public class OrderController {
    
    @GetMapping("/orders/{id}")
    public Order getOrder(@PathVariable Long id) {
        // 每个请求在独立虚拟线程中执行
        // 即使 10000 并发也不会耗尽线程
        
        // 调用远程服务（阻塞但无影响）
        User user = userService.getUser();
        Payment payment = paymentService.getPayment(id);
        
        return new Order(user, payment);
    }
}
```

### 4.2 IO 密集型任务

```java
public class IOIntensiveTasks {
    
    // 批量下载任务
    public List<String> downloadUrls(List<String> urls) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            List<Callable<String>> tasks = urls.stream()
                .map(url -> (Callable<String>) () -> download(url))
                .toList();
            
            // 同时启动所有下载（虚拟线程自动调度）
            List<Future<String>> futures = executor.invokeAll(tasks);
            
            // 收集结果
            List<String> results = new ArrayList<>();
            for (Future<String> future : futures) {
                results.add(future.get());
            }
            return results;
        }
    }
    
    // 每个下载在虚拟线程中执行
    private String download(String url) {
        // HTTP 请求阻塞时不会占用 OS 线程
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build();
        
        try {
            HttpResponse<String> response = client.send(
                request, 
                HttpResponse.BodyHandlers.ofString()
            );
            return response.body();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

### 4.3 数据库操作

```java
public class DatabaseOperations {
    
    public List<User> batchQuery(List<Long> ids) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            List<CompletableFuture<User>> futures = ids.stream()
                .map(id -> CompletableFuture.supplyAsync(
                    () -> queryUser(id), 
                    executor  // 虚拟线程执行器
                ))
                .toList();
            
            return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        }
    }
    
    private User queryUser(Long id) {
        // JDBC 操作在虚拟线程中执行
        // 等待数据库响应时释放载体线程
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT * FROM users WHERE id = ?")) {
            
            ps.setLong(1, id);
            
            // 阻塞操作自动处理
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return new User(rs.getLong("id"), rs.getString("name"));
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
```

### 4.4 不适合的场景

```java
// ❌ 场景1：纯 CPU 密集型任务
public void cpuIntensiveTask() {
    // CPU 密集型任务使用虚拟线程没有优势
    // 因为计算时虚拟线程也会占用 OS 线程
    // 适合：ForkJoinPool.commonPool() 或自定义线程池
}

// ❌ 场景2：使用 synchronized 的代码
public synchronized void badMethod() {
    // synchronized 会占用 OS 线程（不卸载）
    // 在虚拟线程中尽量使用 ReentrantLock
}

// ✅ 解决方案：使用 ReentrantLock
private final ReentrantLock lock = new ReentrantLock();

public void goodMethod() {
    lock.lock();
    try {
        // 虚拟线程阻塞在 ReentrantLock 时会释放载体线程
    } finally {
        lock.unlock();
    }
}
```

---

## 5. 与传统线程池对比

### 5.1 对比表

| 特性 | ThreadPoolExecutor | ForkJoinPool | Virtual Thread |
|------|-------------------|--------------|----------------|
| **线程类型** | Platform Thread | Platform Thread | Virtual Thread |
| **内存占用** | 1MB+ | 1MB+ | ~500 bytes |
| **最大数量** | ~10000 | ~CPU 核心数 | 百万级 |
| **创建开销** | ~1ms | ~1ms | ~1μs |
| **阻塞时** | 占用线程等待 | 工作窃取 | 自动挂起/恢复 |
| **适用场景** | CPU/IO 混合 | CPU 密集型 | IO 密集型 |
| **编程模型** | 需要管理队列 | 分治递归 | 顺序代码 |

### 5.2 性能对比测试

```java
public class PerformanceTest {
    
    // 测试：创建 10 万个任务，每个睡眠 1 秒
    public static void main(String[] args) throws Exception {
        int taskCount = 100_000;
        Duration duration = Duration.ofSeconds(1);
        
        // 1. 传统线程池遍历
        // 线程池大小 100：耗时 1000 秒（串行）
        // 线程池大小 10000：OOM
        
        // 2. 虚拟线程（推荐）
        long start = System.currentTimeMillis();
        
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            
            for (int i = 0; i < taskCount; i++) {
                futures.add(executor.submit(() -> {
                    Thread.sleep(duration);
                    return null;
                }));
            }
            
            // 等待所有完成
            for (Future<?> f : futures) {
                f.get();
            }
        }
        
        long elapsed = System.currentTimeMillis() - start;
        // 实际耗时约 101 秒（10万任务在约1000个线程上并行）
        System.out.println("Completed in " + elapsed + "ms");
    }
}
```

**测试结果对比：**

| 方案 | 线程池大小 | 10万任务耗时 | 内存占用 |
|------|-----------|-------------|---------|
| 传统线程池 | 100 | 1000秒（串行） | ~100MB |
| 传统线程池 | 10000 | OOM | ~10GB |
| **虚拟线程** | 自适应 | **~101秒** | **~50MB** |

### 5.3 线程池选择决策

```
┌───────────────────────────────────────────────┐
│              任务类型判断                      │
└───────────────┬───────────────────────────────┘
                │
        ┌───────┴───────┐
        ▼               ▼
   CPU 密集型      IO 密集型
   (计算为主)       (等待为主)
        │               │
        ▼               ▼
┌───────────────┐   ┌──────────────────┐
│ ForkJoinPool  │   │ 虚拟线程 Executor │
│ 或计算专用池   │   │                  │
│               │   │ 高并发网络请求    │
│ 数据处理       │   │ 数据库操作        │
│ 图像计算       │   │ 文件 IO          │
│ 算法执行       │   │ HTTP 调用        │
└───────────────┘   └──────────────────┘
        │
        ▼
┌───────────────────────────────┐
│ 混合场景（CPU + IO）          │
│ Platform Thread Pool         │
│ 配合 Virtual Thread 执行 IO  │
└───────────────────────────────┘
```

---

## 6. 最佳实践

### 6.1 使用模式

```java
// ✅ 模式1：每个任务一个虚拟线程（推荐）
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    // 自动管理虚拟线程生命周期
    executor.submit(() -> task());
}

// ✅ 模式2：批量提交任务
public <T> List<T> executeAll(List<Callable<T>> tasks) {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        List<Future<T>> futures = executor.invokeAll(tasks);
        
        List<T> results = new ArrayList<>();
        for (Future<T> future : futures) {
            results.add(future.get());
        }
        return results;
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}

// ✅ 模式3：CompletableFuture + 虚拟线程
CompletableFuture.supplyAsync(
    () -> fetchData(),
    Executors.newVirtualThreadPerTaskExecutor()
).thenApply(this::process)
 .thenAccept(this::save);
```

### 6.2 注意事项

```java
public class BestPractices {
    
    // ⚠️ 1. 不要在虚拟线程中使用 ThreadLocal（可能导致内存问题）
    // 使用 ScopedValue 替代
    
    // ⚠️ 2. 避免使用 synchronized
    // 改用 ReentrantLock 或 juc 包中的锁
    
    private final ReentrantLock lock = new ReentrantLock();
    
    public void safeLock() {
        lock.lock();
        try {
            // 保护代码
        } finally {
            lock.unlock();
        }
    }
    
    // ⚠️ 3. 合理使用线程数量
    // 虚拟线程便宜但也不是免费的
    public void limitedConcurrency() {
        Semaphore semaphore = new Semaphore(100);
        
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, 1000).forEach(i -> {
                executor.submit(() -> {
                    semaphore.acquire();
                    try {
                        // 限制并发数的操作
                        callExternalAPI();
                    } finally {
                        semaphore.release();
                    }
                });
            });
        }
    }
}
```

### 6.3 调试与监控

```java
public class Debugging {
    
    // 查看当前线程是否是虚拟线程
    public void checkThread() {
        Thread current = Thread.currentThread();
        System.out.println("Is Virtual: " + current.isVirtual());
        System.out.println("Thread Name: " + current.getName());
    }
    
    // 使用 jcmd 监控
    // jcmd <pid> Thread.print
    
    // JFR 事件（JDK Flight Recorder）
    // - jdk.VirtualThreadStart
    // - jdk.VirtualThreadEnd
    // - jdk.VirtualThreadPinned
}
```

---

## 7. 常见问题

### Q1：虚拟线程 vs 协程（Goroutine）有什么不同？

| 特性 | Java 虚拟线程 | Go Goroutine |
|------|--------------|--------------|
| **调度器** | JVM 调度 | Go Runtime 调度 |
| **栈大小** | 动态扩容（初始很小） | 2KB 初始，自动增长 |
| **阻塞时** | 自动卸载到 JVM 堆 | Go 调度器切换 |
| **兼容性** | 100% 兼容 Thread API | 独立语法 |
| **着色函数** | 无需（透明） | 需要 async/await |

### Q2：虚拟线程可以完全替代线程池吗？

```
答案：不能，看场景

虚拟线程适合：
✅ IO密集型（网络、数据库）
✅ 高并发轻量级任务
✅ 同步代码异步化

传统线程适合：
✅ CPU密集型计算
✅ 需要精确控制的场景
✅ 遗留代码依赖 ThreadLocal/synchronized
```

### Q3：如何迁移现有代码到虚拟线程？

```java
// 步骤1：将线程池改为虚拟线程执行器

// 原代码
ExecutorService executor = Executors.newFixedThreadPool(100);

// 新代码
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

// 步骤2：替换 synchronized 为 ReentrantLock

// 步骤3：替换 ThreadLocal 为 ScopedValue

// 步骤4：性能测试和监控
```

### Q4：虚拟线程是否存在内存泄漏风险？

```java
// ⚠️ 潜在问题：大量虚拟线程创建但引用被持有

List<Thread> threads = new ArrayList<>();

// 创建了 100 万个虚拟线程并保存引用
for (int i = 0; i < 1_000_000; i++) {
    Thread vt = Thread.startVirtualThread(() -> {
        // 长时间运行任务
        while (true) {
            Thread.sleep(1000);
        }
    });
    threads.add(vt); // 保存引用
}

// 解决方案：
// 1. 使用 try-with-resources 或 ExecutorService 自动管理
// 2. 避免长时间运行的虚拟线程（必要时用传统线程）
// 3. 及时清理 ThreadLocal/ScopedValue
```

---

## 总结

### 虚拟线程核心价值

```java
┌─────────────────────────────────────────────┐
│           虚拟线程核心价值                  │
├─────────────────────────────────────────────┤
│                                             │
│  1. 并发能力：从 1000 线程 → 100万+ 线程    │
│                                             │
│  2. 资源效率：1MB+ → 500字节 / 线程        │
│                                             │
│  3. 编程简化：告别 Callback Hell           │
│              顺序代码实现异步能力            │
│                                             │
│  4. 兼容升级：现有代码无感知迁移            │
│                                             │
└─────────────────────────────────────────────┘
```

### 面试金句

> "Java 21 虚拟线程是 Project Loom 的核心特性，它实现了 M:N 调度模型，可以在 JVM 内部创建百万级轻量级线程。虚拟线程在遇到 IO 阻塞时会自动挂起并释放 OS 线程，这个机制特别适合高并发 IO 密集型场景，如 Web 服务、数据库操作等。与传统线程池相比，虚拟线程可以大幅降低系统资源消耗并简化并发编程模型，但并不会替代传统线程池在 CPU 密集型场景下的作用。"

---

**参考资料：**
- JEP 444: Virtual Threads (Java 21)
- Project Loom: https://openjdk.org/projects/loom/
- Oracle Blog: https://blogs.oracle.com/javamagazine/

**适用版本：** Java 21+