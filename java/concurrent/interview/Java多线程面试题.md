interview-bible\java\md\Java多线程面试题.md
# Java 多线程面试题

## 目录
1. [基础概念](#基础概念)
2. [线程创建方式](#线程创建方式)
3. [线程状态](#线程状态)
4. [线程安全](#线程安全)
5. [线程间通信](#线程间通信)
6. [线程池](#线程池)
7. [并发工具类](#并发工具类)
9. [死锁](#死锁)
10. [代码示例](../code/ThreadCreationDemo.java)

---

## 基础概念

### 1. 进程和线程的区别？

| 特性 | 进程 (Process) | 线程 (Thread) |
|------|---------------|---------------|
| **资源占用** | 独立地址空间，资源占用大 | 共享进程资源，资源占用小 |
| **通信方式** | IPC（管道、Socket、共享内存等） | 直接读写共享内存 |
| **切换开销** | 大（需要切换页表） | 小（只需切换栈和寄存器） |
| **并发性** | 进程间并发 | 线程间并发 |
| **安全性** | 进程间隔离，更安全 | 共享内存，可能互相影响 |

### 2. 为什么要使用多线程？

**核心优势：**

1. **提高 CPU 利用率**
   - 当一个线程等待 I/O 时，其他线程可以继续执行

2. **提升系统吞吐量**
   - 多个任务并行执行，缩短整体处理时间

3. **更好的响应性**
   - GUI 应用保持界面响应，后台执行耗时任务

4. **资源高效利用**
   - 线程切换比进程切换开销小

### 3. 并发(Concurrency) vs 并行(Parallelism)？

```
并发：多个任务交替执行，微观上串行，宏观上并行
      [任务A]---[任务B]---[任务A]---[任务B]
      单核 CPU 通过时间片轮转实现并发

并行：多个任务真正同时执行
      [任务A]===========>
      [任务B]===========>
      多核 CPU 可以同时执行多个任务
```

---

## 线程创建方式

### 4. Java 创建线程的几种方式？

**方式一：继承 Thread 类**
```java
class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Thread running: " + getName());
    }
}

MyThread t1 = new MyThread();
t1.start();
```

**方式二：实现 Runnable 接口（推荐）**
```java
class MyRunnable implements Runnable {
    @Override
    public void run() {
        System.out.println("Runnable running: " + Thread.currentThread().getName());
    }
}

Thread t2 = new Thread(new MyRunnable());
t2.start();
```

**方式三：实现 Callable 接口 + FutureTask**
```java
class MyCallable implements Callable<String> {
    @Override
    public String call() throws Exception {
        return "Callable result";
    }
}

FutureTask<String> futureTask = new FutureTask<>(new MyCallable());
new Thread(futureTask).start();
String result = futureTask.get(); // 阻塞等待结果
```

**方式四：线程池（推荐，生产环境使用）**
```java
ExecutorService executor = Executors.newFixedThreadPool(5);
executor.execute(() -> System.out.println("Pool thread running"));
executor.shutdown();
```

### 5. Runnable vs Callable 的区别？

| 特性 | Runnable | Callable |
|------|----------|----------|
| 返回值 | 无返回值 | 有返回值 |
| 异常处理 | 不能抛出受检异常 | 可以抛出受检异常 |
| 方法 | `run()` | `call()` |
| 使用方式 | `new Thread(runnable).start()` | `FutureTask` 或线程池 |

---

## 线程状态

### 6. Java 线程的 6 种状态？

```java
public enum State {
    NEW,        // 新建：线程被创建，未启动
    RUNNABLE,   // 可运行：正在运行或在等待 CPU 时间片
    BLOCKED,    // 阻塞：等待监视器锁（synchronized）
    WAITING,    // 等待：无限期等待（wait/join/park）
    TIMED_WAITING, // 超时等待：有限期等待（sleep/wait(timeout)）
    TERMINATED  // 终止：线程执行完毕
}
```

**状态转换图：**
```
NEW
 |
 | start()
 v
RUNNABLE <-----> BLOCKED (等待锁)
 | ^
 | | wait()
 | v
 | WAITING <----> TIMED_WAITING (sleep/wait(timeout))
 | | notify()/notifyAll()
 | |
 v |
TERMINATED
```

### 7. sleep() vs wait() 的区别？

| 特性 | sleep() | wait() |
|------|---------|--------|
| 所属类 | Thread 类（静态方法） | Object 类（实例方法） |
| 锁行为 | 不释放锁 | 释放锁 |
| 使用场景 | 暂停执行一段时间 | 线程间通信/条件等待 |
| 唤醒方式 | 时间到自动唤醒 | 需要 notify()/notifyAll() |
| 使用位置 | 任意地方 | 必须在同步代码块内 |

---

## 线程安全

### 8. 什么是线程安全？如何保证线程安全？

**线程安全定义：** 多个线程同时访问一个类时，如果不需要额外的同步协调，这个类仍然能表现出正确的行为。

**保证线程安全的方法：**

1. **互斥同步（阻塞）**
   - synchronized
   - ReentrantLock

2. **非阻塞同步**
   - CAS（Compare-And-Swap）
   - Atomic 原子类

3. **无同步方案**
   - ThreadLocal（线程隔离）
   - 不可变对象（final）
   - 使用线程安全类（ConcurrentHashMap 等）

### 9. synchronized 的三种使用方式？

```java
public class SynchronizedDemo {
    
    // 1. 同步实例方法：锁对象是当前实例
    public synchronized void method1() {
        // ...
    }
    
    // 2. 同步代码块：锁对象是指定对象
    public void method2() {
        synchronized (this) {
            // ...
        }
    }
    
    // 3. 同步静态方法：锁对象是当前类的 Class 对象
    public static synchronized void method3() {
        // ...
    }
    
    // 4. 同步静态代码块：锁对象是类的 Class 对象
    public static void method4() {
        synchronized (SynchronizedDemo.class) {
            // ...
        }
    }
}
```

### 10. synchronized 底层原理？

**字节码层面：**
- 同步方法：ACC_SYNCHRONIZED 标志位
- 同步代码块：monitorenter 和 monitorexit 指令

**Monitor 机制：**
```
每个 Java 对象都有一个关联的 Monitor（监视器锁）
- monitorenter：尝试获取对象的 Monitor，计数器从 0 变为 1
- monitorexit：释放 Monitor，计数器减 1
- 可重入：同一个线程可以多次获取锁，计数器累加
```

### 11. synchronized vs Lock（ReentrantLock）？

| 特性 | synchronized | ReentrantLock |
|------|--------------|---------------|
| **实现方式** | JVM 层面（监视器锁） | API 层面（AQS 框架） |
| **灵活性** | 自动获取/释放锁 | 手动 lock()/unlock() |
| **功能** | 基础功能 | 更灵活（可中断、超时、公平锁等） |
| **条件变量** | 一个隐式条件（wait/notify） | 多个 Condition |
| **性能** | JDK6+ 优化后接近 | 竞争激烈时略优 |
| **选择性** | 必须使用 | 可选，更灵活 |

### 12. volatile 关键字的作用？

**三个特性：**

1. **可见性**
   - 保证变量的读写对所有线程立即可见
   - 写操作会刷新到主内存，读操作从主内存读取

2. **禁止指令重排序**
   - 防止指令重排序优化导致的问题
   - 插入内存屏障保证指令执行顺序

3. **不保证原子性**
   - `i++` 操作不是原子的（读取-修改-写入）
   - 需要配合 synchronized 或 Atomic 类

** happen-before 规则：**
```
volatile 写操作 happen-before 后续的 volatile 读操作
即：写操作的结果对读操作可见
```

### 13. CAS（Compare-And-Swap）机制？

**原理：**
```java
// 伪代码
int compareAndSwap(int expectedValue, int newValue) {
    int currentValue = memory[offset];
    if (currentValue == expectedValue) {
        memory[offset] = newValue;  // 原子操作
        return true;
    }
    return false;
}
```

**实现：**
- Java：Unsafe 类提供 `compareAndSwapInt` 等方法
- 硬件：CPU 提供原子指令（如 x86 的 cmpxchg）

**优点：**
- 无锁，高性能
- 避免线程切换开销

**缺点（ABA 问题）：**
- A → B → A，CAS 无法检测
- 解决：AtomicStampedReference（带版本号）

### 14. Atomic 原子类有哪些？原理是什么？

**常用类：**

| 类型 | 类名 |
|------|------|
| 基本类型 | AtomicInteger, AtomicLong, AtomicBoolean |
| 数组类型 | AtomicIntegerArray, AtomicReferenceArray |
| 引用类型 | AtomicReference, AtomicStampedReference |
| 字段更新器 | AtomicIntegerFieldUpdater |

**原理：**
- 使用 volatile + CAS（Unsafe 类）
- 循环尝试直到 CAS 成功

```java
// AtomicInteger 的 getAndIncrement 源码
public final int getAndIncrement() {
    return unsafe.getAndAddInt(this, valueOffset, 1);
}

// Unsafe 中的实现（循环 CAS）
public final int getAndAddInt(Object o, long offset, int delta) {
    int v;
    do {
        v = getIntVolatile(o, offset);  // 读取当前值
    } while (!compareAndSwapInt(o, offset, v, v + delta));  // CAS 更新
    return v;
}
```

---

## 线程间通信

### 15. wait()/notify()/notifyAll() 如何使用？

```java
public class ThreadCommunication {
    private boolean flag = false;
    
    public synchronized void produce() throws InterruptedException {
        while (flag) {
            wait(); // 有产品了，等待消费
        }
        // 生产产品
        flag = true;
        notifyAll(); // 通知消费者
    }
    
    public synchronized void consume() throws InterruptedException {
        while (!flag) {
            wait(); // 没有产品，等待生产
        }
        // 消费产品
        flag = false;
        notifyAll(); // 通知生产者
    }
}
```

**注意：**
- 必须在同步代码块内使用
- 使用 `while` 而不是 `if` 防止虚假唤醒
- notifyAll() 比 notify() 更安全

### 16. CountDownLatch、CyclicBarrier、Semaphore 的区别？

| 工具类 | 作用 | 使用场景 | 是否可重用 |
|--------|------|----------|------------|
| **CountDownLatch** | 倒计时门闩，等待指定数量线程完成 | 主线程等待所有子线程完成 | 否 |
| **CyclicBarrier** | 循环栅栏，线程互相等待到达屏障 | 多线程分阶段任务，汇合后继续 | 是 |
| **Semaphore** | 信号量，控制同时访问的线程数量 | 限流、资源池 | 是 |

**示例：**

```java
// CountDownLatch：等待所有线程完成
CountDownLatch latch = new CountDownLatch(5);
for (int i = 0; i < 5; i++) {
    new Thread(() -> {
        // 执行任务
        latch.countDown(); // 计数减 1
    }).start();
}
latch.await(); // 主线程等待

// CyclicBarrier：线程互相等待
CyclicBarrier barrier = new CyclicBarrier(3);
for (int i = 0; i < 3; i++) {
    new Thread(() -> {
        // 阶段 1
        barrier.await(); // 到达屏障，等待其他线程
        // 阶段 2
    }).start();
}

// Semaphore：控制并发数量
Semaphore semaphore = new Semaphore(10); // 最大并发 10
semaphore.acquire();  // 获取许可
semaphore.release(); // 释放许可
```

---

## 线程池

### 17. 为什么要用线程池？

**优势：**
1. **降低资源消耗**：重复利用已创建的线程
2. **提高响应速度**：任务到达立即执行
3. **便于管理**：控制并发线程数，防止资源耗尽
4. **支持扩展**：如定时执行、周期执行

### 18. ThreadPoolExecutor 的核心参数？

```java
public ThreadPoolExecutor(
    int corePoolSize,      // 核心线程数：常驻线程数量
    int maximumPoolSize,   // 最大线程数：线程池最大容量
    long keepAliveTime,    // 非核心线程空闲存活时间
    TimeUnit unit,         // 时间单位
    BlockingQueue<Runnable> workQueue,  // 任务队列
    ThreadFactory threadFactory,          // 线程工厂
    RejectedExecutionHandler handler      // 拒绝策略
)
```

**执行流程：**
```
1. 核心线程未满 -> 创建新线程执行任务
2. 核心线程已满 -> 任务进入队列
3. 队列已满 -> 创建非核心线程（不超过 maximumPoolSize）
4. 线程数达上限且队列满 -> 执行拒绝策略
```

### 19. 线程池的拒绝策略有哪些？

| 策略 | 说明 |
|------|------|
| **AbortPolicy**（默认） | 抛出 RejectedExecutionException |
| **CallerRunsPolicy** | 由调用线程（提交任务的线程）执行 |
| **DiscardPolicy** | 静默丢弃任务 |
| **DiscardOldestPolicy** | 丢弃队列中最老的任务，然后重试提交 |

### 20. 线程池的常用类型？

**Executors 工厂方法（不推荐，有 OOM 风险）：**

```java
// 固定大小线程池
ExecutorService fixed = Executors.newFixedThreadPool(10);

// 单线程线程池
ExecutorService single = Executors.newSingleThreadExecutor();

// 可缓存线程池（线程数无上限）
ExecutorService cached = Executors.newCachedThreadPool();

// 定时任务线程池
ScheduledExecutorService scheduled = Executors.newScheduledThreadPool(5);
```

**推荐使用 ThreadPoolExecutor 手动创建：**

```java
// 推荐做法：明确指定参数
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5,                      // 核心线程数
    10,                     // 最大线程数
    60L,                    // 空闲线程存活时间
    TimeUnit.SECONDS,       // 时间单位
    new LinkedBlockingQueue<>(100),  // 有界队列（防止 OOM）
    new ThreadFactory() {           // 自定义线程工厂
        private AtomicInteger count = new AtomicInteger(0);
        public Thread newThread(Runnable r) {
            return new Thread(r, "custom-pool-" + count.incrementAndGet());
        }
    },
    new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
);
```

### 21. 如何优雅地关闭线程池？

```java
// 1. 优雅关闭：不再接受新任务，等待已提交任务完成
executor.shutdown();

// 2. 等待指定时间
if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
    // 超时后强制关闭
    executor.shutdownNow();
}

// 3. 再次等待
if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
    System.err.println("线程池未正常关闭");
}
```

---

## 并发工具类

### 22. ConcurrentHashMap 如何保证线程安全？

**JDK 7：分段锁（Segment）**
- 将数据分成多个 Segment，每个 Segment 独立的锁
- 锁粒度更细，并发度更高

**JDK 8：CAS + synchronized**
- 使用 CAS 操作保证无冲突时的性能
- 冲突时用 synchronized 锁住链表/红黑树头节点
- 锁粒度更细（每个桶独立）

**与 Hashtable 的区别：**
- Hashtable：synchronized 方法锁，锁整个对象，性能差
- ConcurrentHashMap：分段/桶级锁，并发度高

### 23. BlockingQueue 常用实现？

| 实现类 | 特点 |
|--------|------|
| **ArrayBlockingQueue** | 有界数组实现，固定容量 |
| **LinkedBlockingQueue** | 链表实现，默认无界（可指定有界） |
| **PriorityBlockingQueue** | 支持优先级排序 |
| **SynchronousQueue** | 不存储元素，直接传递 |
| **DelayQueue** | 延迟队列，元素到期后才能取出 |

---

## 死锁

### 24. 什么是死锁？产生的条件？

**死锁定义：** 多个线程互相持有对方需要的资源，形成循环等待，导致所有线程无法继续执行。

**死锁四个必要条件：**
1. **互斥条件**：资源一次只能被一个线程占用
2. **请求与保持**：线程持有资源同时申请新资源
3. **不可剥夺**：资源不能被强制剥夺
4. **循环等待**：线程间形成等待环路

### 25. 如何避免死锁？

**1. 破坏请求与保持条件**
```java
// 一次性申请所有资源
synchronized(resourceA) {
    synchronized(resourceB) {
        // 操作资源
    }
}
```

**2. 按固定顺序加锁**
```java
// 所有线程都按 A -> B 的顺序加锁
int hashA = System.identityHashCode(lockA);
int hashB = System.identityHashCode(lockB);
if (hashA < hashB) {
    synchronized(lockA) {
        synchronized(lockB) { /* ... */ }
    }
} else {
    synchronized(lockB) {
        synchronized(lockA) { /* ... */ }
    }
}
```

**3. 使用定时锁（Lock.tryLock()）**
```java
if (lock.tryLock(3, TimeUnit.SECONDS)) {
    try {
        // 操作
    } finally {
        lock.unlock();
    }
} else {
    // 获取锁超时，放弃或重试
}
```

**4. 使用并发工具类**
- 使用 `ConcurrentHashMap` 代替 `HashTable + synchronized`
- 使用 `CopyOnWriteArrayList` 等线程安全集合

---

## 高级话题

### 26. AQS（AbstractQueuedSynchronizer）是什么？

**AQS** 是 Java 并发包的核心基础框架，JUC 中大部分同步工具都是基于 AQS 实现的。

**核心思想：**
- 使用 **int state** 表示同步状态
- 使用 **CLH 队列** 管理等待线程
- 子类通过实现 tryAcquire/tryRelease 方法定义获取/释放逻辑

**基于 AQS 的实现：**
- ReentrantLock
- CountDownLatch
- Semaphore
- CyclicBarrier

### 27. Fork/Join 框架是什么？

**原理：** 工作窃取（Work Stealing）
- 大任务拆分成小任务，在多个线程并行执行
- 线程池中的线程可以从其他线程的队列中"窃取"任务

```java
ForkJoinPool pool = new ForkJoinPool();
ForkJoinTask<Integer> result = pool.submit(new RecursiveTask<Integer>() {
    @Override
    protected Integer compute() {
        // 任务拆分逻辑
        if (任务足够小) {
            return 直接计算;
        } else {
            ForkJoinTask<Integer> left = new 子任务().fork();
            ForkJoinTask<Integer> right = new 子任务().fork();
            return left.join() + right.join();
        }
    }
});
```

---

## 面试技巧

### 高频面试题 TOP 10

1. **线程池的核心参数及执行流程？**
2. **synchronized 的锁升级过程（无锁->偏向锁->轻量级锁->重量级锁）？**
3. ** volatile 能保证线程安全吗？为什么？**
4. **CAS 的 ABA 问题及解决方案？**
5. **ConcurrentHashMap 的并发原理（JDK7 vs JDK8）？**
6. **ThreadLocal 原理及内存泄漏原因？**
7. **死锁产生的条件及如何避免？**
8. **AQS 的原理及 CountDownLatch 的实现？**
9. **wait()/notify() 和 Condition 的区别？**
10. **如何设计一个高并发的秒杀系统？**

---

## 代码示例

> 点击查看完整代码示例：
> - [ThreadCreationDemo.java](../code/ThreadCreationDemo.java) - 线程创建与基础使用
> - [ThreadLocalMemoryLeakDemo.java](../code/ThreadLocalMemoryLeakDemo.java) - ThreadLocal 内存泄漏
> - [ThreadPoolDemo.java](../code/ThreadPoolDemo.java) - 线程池使用

---

*本文档持续更新，建议收藏备用*