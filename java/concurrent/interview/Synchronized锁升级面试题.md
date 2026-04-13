# synchronized 和 Lock 锁机制面试题

## 目录
1. [synchronized vs Lock 对比](#synchronized-vs-lock-对比)
2. [synchronized 底层实现](#synchronized-底层实现)
3. [锁升级过程](#锁升级过程)
4. [锁消除与锁粗化](#锁消除与锁粗化)
5. [代码示例](#代码示例)

---

## synchronized vs Lock 对比

### 1. synchronized 和 Lock 有什么区别？

**答案：**

| 特性 | synchronized | Lock（ReentrantLock）|
|------|-------------|---------------------|
| **实现方式** | JVM 层面（字节码指令） | API 层面（Java 代码） |
| **灵活性** | 自动获取/释放锁 | 手动 lock()/unlock() |
| **可中断** | 不能响应中断 | 可以 lockInterruptibly() |
| **超时等待** | 不支持 | tryLock(long, TimeUnit) |
| **公平锁** | 非公平 | 可选公平/非公平 |
| **条件变量** | 一个（wait/notify） | 多个 Condition |
| **性能** | JDK6+ 大幅优化 | 竞争激烈时略优 |
| **代码要求** | 必须完整执行 | 必须在 finally 释放锁 |

**使用示例：**

```java
// synchronized 方式
public synchronized void method() {
    // 自动释放锁
}

// Lock 方式
private final Lock lock = new ReentrantLock();

public void method() {
    lock.lock();
    try {
        // 业务逻辑
    } finally {
        lock.unlock(); // 必须手动释放
    }
}
```

---

## synchronized 底层实现

### 2. synchronized 在字节码层面是如何实现的？

**答案：**

**字节码指令：**

```java
public class SynchronizedDemo {
    // 同步方法
    public synchronized void method() {
        // ACC_SYNCHRONIZED 标志位
    }
    
    // 同步代码块
    public void block() {
        synchronized (this) {
            // monitorenter + monitorexit
        }
    }
}
```

**编译后的字节码：**

```java
// 同步方法
public synchronized void method();
    flags: ACC_PUBLIC, ACC_SYNCHRONIZED  // JVM 通过标志位识别
    
// 同步代码块
public void block();
    Code:
       0: aload_0
       1: dup
       2: astore_1
       3: monitorenter        // 进入同步块，获取锁
       4: aload_1
       5: monitorexit         // 正常退出，释放锁
      12: monitorexit         // 异常退出，释放锁
```

**Monitor（监视器锁）机制：**

```
每个 Java 对象都有一个关联的 Monitor
- 对象头中的 Mark Word 指向 Monitor
- Monitor 包含：
  ├── Owner：持有锁的线程
  ├── EntryList：等待获取锁的线程队列
  ├── WaitSet：调用 wait() 等待的线程队列
  └── Recursions：重入次数
```

---

## 锁升级过程

### 3. synchronized 的锁升级过程是什么？

**答案：**

JDK6 引入的锁优化，锁状态存储在对象头的 Mark Word 中。

**锁的四种状态：**

```
无锁 → 偏向锁 → 轻量级锁 → 重量级锁
（只能升级，不能降级，除了偏向锁可以撤销）
```

**对象头结构（64位 JVM）：**

```
|--------------------------------------------------------------------------------------|
|                                  Mark Word (64 bits)                                 |
|--------------------------------------------------------------------------------------|
| 锁状态   | 25 bit | 31 bit          | 1 bit | 4 bit | 1 bit | 2 bit |
| 无锁     | unused | hashCode        | unused | 分代年龄 | 0   | 01  |
| 偏向锁   | 线程ID | Epoch           | unused | 分代年龄 | 1   | 01  |
| 轻量级锁 |                        指向栈中锁记录的指针 (Lock Record)              | 00  |
| 重量级锁 |                        指向互斥量( monitor )的指针                  | 10  |
| GC标记   |                        空                                              | 11  |
```

---

### 4. 偏向锁是什么？什么时候会升级为轻量级锁？

**答案：**

**偏向锁（Biased Locking）：**

**核心思想：** 为了让线程获得锁的代价更低，如果只有一个线程访问同步块，直接将锁偏向于该线程，避免 CAS 操作。

**原理：**
```java
// 第一次获取锁
线程A 访问同步块
    ↓
检查 Mark Word：是否为偏向锁（最后3位是否为101）
    ↓
否：CAS 替换 Mark Word，将线程ID设为自己，设置偏向锁标志
是：检查线程ID是否是自己
    ├─ 是：直接执行（无需任何同步操作！）
    └─ 否：说明有其他线程竞争，升级为轻量级锁
```

**适用场景：**
- 单线程反复访问同步块
- 没有竞争或很少竞争的场景

**升级触发条件：**
1. 另一个线程尝试获取偏向锁（先撤销偏向，再升级）
2. 调用 hashCode()（需要用到 Mark Word 存储 hashCode）

**JVM 参数：**
```bash
# 开启偏向锁（JDK6+ 默认开启，延迟4秒加载）
-XX:+UseBiasedLocking

# 设置偏向锁延迟时间为0（立即生效）
-XX:BiasedLockingStartupDelay=0

# 关闭偏向锁
-XX:-UseBiasedLocking
```

---

### 5. 轻量级锁是什么？什么时候升级为重量级锁？

**答案：**

**轻量级锁（Lightweight Locking）：**

**核心思想：** 在没有多线程竞争或竞争不激烈时，避免使用重量级锁（操作系统 Mutex），使用 CAS 操作代替。

**加锁过程：**

```java
// 线程进入同步块
1. 在栈帧中创建 Lock Record（锁记录）
   Lock Record:
   ├── displaced_mark_word (备份原 Mark Word)
   └── owner (指向锁对象)

2. CAS 将对象的 Mark Word 替换为指向 Lock Record 的指针
   成功：获取锁，Mark Word 最后2位变为 00（轻量级锁）
   失败：检查是否是自己持有
        ├─ 是：重入（Lock Record 数量+1）
        └─ 否：有竞争，升级为重量级锁（自适应自旋后）

3. 执行同步代码

4. 解锁：CAS 将 Mark Word 恢复为备份的值
   成功：解锁完成
   失败：说明有竞争，需要重量级锁的释放流程
```

**自适应自旋：**
```java
// 线程获取轻量级锁失败时，不会立即升级
// 而是自旋等待（忙等待），期望持有锁的线程很快释放

自旋次数 = 自适应算法决定（根据历史成功率）
- 上次自旋成功：增加自旋次数（最多60次）
- 上次自旋失败：减少自旋次数或直接升级

JVM 参数：
-XX:+UseSpinning      # 开启自旋（JDK6+ 默认）
-XX:PreBlockSpin=10  # 默认自旋10次
```

**升级重量级锁条件：**
1. 自旋超过阈值仍未获得锁
2. 竞争线程超过1个（第三个线程来竞争）

---

### 6. 重量级锁是什么？

**答案：**

**重量级锁（Heavyweight Locking）：**

**核心思想：** 当竞争严重时，使用操作系统提供的互斥量（Mutex），线程阻塞和唤醒需要用户态和内核态切换。

**Monitor 结构：**
```c
// hotspot 源码中的 ObjectMonitor
ObjectMonitor() {
    _header       = NULL;  // 对象头
    _count        = 0;     // 重入次数
    _waiters      = 0,     // wait的线程数
    _recursions   = 0;     // 嵌套层数
    _object       = NULL;  // 锁对象
    _owner        = NULL;  // 持有锁的线程
    _WaitSet      = NULL;  // wait的线程队列（双向链表）
    _WaitSetLock  = 0 ;
    _Responsible  = NULL ;
    _succ         = NULL ;
    _cxq          = NULL ;  // 竞争队列（单链表，栈结构）
    FreeNext      = NULL ;
    _EntryList    = NULL ; // 入口等待队列（双向链表）
    _SpinFreq     = 0 ;
    _SpinClock    = 0 ;
    OwnerIsThread = 0 ;
}
```

**线程状态转换：**
```
线程A（持有锁）    线程B（竞争锁）    线程C（调用wait）
     │                  │                 │
     ▼                  ▼                 ▼
  Owner              cxq/EntryList      WaitSet
（执行中）            （阻塞等待）       （等待通知）
```

**锁获取流程：**
```java
// 重量级锁获取过程
1. 通过对象头的 Mark Word 找到 Monitor

2. CAS 尝试将 Monitor 的 Owner 设为自己
   成功：获得锁
   失败：进入竞争队列（cxq，栈结构）

3. 竞争失败的线程被阻塞（park）
   从用户态切换到内核态（开销大！）

4. 锁释放时，从 cxq 或 EntryList 中唤醒线程
   被唤醒的线程重新竞争（非公平）
```

**为什么重量级锁开销大？**
- 用户态 ⇄ 内核态切换（约 100 个 CPU 时钟周期）
- 线程上下文切换
- CPU 缓存失效

---

### 7. 锁升级过程的完整流程图？

**答案：**

```
对象创建（无锁状态）
   │
   ▼
新对象（Mark Word：无锁 + hashCode位置为0）
   │ 第一个线程访问同步块
   │ 检查：是否可偏向（anonymous biased）
   ▼
偏向锁（Biased）
   │ 线程ID设为当前线程
   │ 标志位：101
   │
   ├─────────────────────────────────────┐
   │                                     │
   │ 同一线程再次进入                      │ 其他线程尝试获取
   │ 检查：线程ID是否匹配                  │ 检查：线程ID不匹配
   │ 匹配：直接执行（无开销！）             │ mark word 无hashCode
   │                                     │ 撤销偏向锁 → 升级为轻量级锁
   ▼                                     │
再次进入（无CAS，极快）                    │
   │                                     │
   │ 调用 hashCode()                      │
   │ 需要存储hashCode，撤销偏向            │
   ▼                                     │
轻量级锁（Lightweight）◄──────────────────┘
   │ CAS 替换 Mark Word 为指向 Lock Record 的指针
   │ 标志位：00
   │
   ├─────────────────────────────────────┐
   │                                     │
   │ CAS 成功                            │ CAS 失败（已被其他线程持有）
   │ 获取锁，执行代码                     │
   │                                     │ 自旋等待（自适应次数）
   │ CAS 解锁                            │
   │ 恢复 Mark Word                       │ 自旋成功：获得轻量级锁
   │                                     │
   ▼                                     │ 自旋失败：升级为重量级锁
释放锁（恢复为无锁或偏向）                 │
                                         ▼
                                    重量级锁（Heavyweight）
                                         │ 通过 Monitor 获取
                                         │ 线程阻塞（park）
                                         │
                                         ▼
                                    释放锁（unpark 等待线程）
```

---

## 锁消除与锁粗化

### 8. 除了锁升级，JDK 还有哪些锁优化？

**答案：**

**1. 锁消除（Lock Elimination）**

```java
// 示例：StringBuffer 的 append 方法有 synchronized
// 但如果在局部变量使用，不存在线程安全问题

public String concat(String s1, String s2) {
    StringBuffer sb = new StringBuffer(); // 局部变量，不会逃逸
    sb.append(s1);  // sb对象不会被其他线程访问
    sb.append(s2);  // synchronized 可以被消除
    return sb.toString();
}

// JIT 编译器通过逃逸分析：
// - sb 只在方法内使用，不会逃逸到其他线程
// - 可以安全地消除 synchronized
```

**JVM 参数：**
```bash
-XX:+DoEscapeAnalysis  # 开启逃逸分析（JDK8+ 默认开启）
```

**2. 锁粗化（Lock Coarsening）**

```java
// 优化前：频繁加锁解锁
public void method() {
    synchronized(lock) {
        // 操作 1
    }
    synchronized(lock) {
        // 操作 2
    }
    synchronized(lock) {
        // 操作 3
    }
    // 每次都有加锁解锁开销
}

// 优化后：合并为一个锁
public void method() {
    synchronized(lock) {
        // 操作 1
        // 操作 2
        // 操作 3
    }
}
// 减少锁的获取和释放次数
```

**3. 自适应自旋（Adaptive Spinning）**

之前已经介绍，根据历史自旋成功率动态调整自旋次数。

---

## Lock 接口实现

### 9. ReentrantLock 的实现原理是什么？

**答案：**

**核心组件：**
```java
// ReentrantLock 内部使用 AQS（AbstractQueuedSynchronizer）
public class ReentrantLock implements Lock, java.io.Serializable {
    private final Sync sync;
    
    // Sync 继承 AQS
    abstract static class Sync extends AbstractQueuedSynchronizer {
        // 实现获取/释放锁的逻辑
    }
}
```

**AQS 结构：**
```java
// AbstractQueuedSynchronizer
public abstract class AbstractQueuedSynchronizer {
    private volatile int state;  // 同步状态（0：未锁定，>0：重入次数）
    
    // 等待队列（CLH 变体）
    private transient volatile Node head;
    private transient volatile Node tail;
    
    static final class Node {
        volatile int waitStatus;    // 等待状态（CANCELLED/SIGNAL/CONDITION/PROPAGATE）
        volatile Node prev;           // 前驱节点
        volatile Node next;           // 后继节点
        volatile Thread thread;       // 绑定的线程
        Node nextWaiter;              // 下一个等待节点（共享/独占模式）
    }
}
```

**加锁过程：**
```java
// ReentrantLock.lock()
public void lock() {
    sync.lock();
}

// 公平锁实现
final void lock() {
    acquire(1);  // AQS 获取1个资源
}

public final void acquire(int arg) {
    // tryAcquire：尝试获取锁（子类实现）
    // 失败则加入到等待队列，并阻塞线程
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}

// 尝试获取锁（公平锁）
protected final boolean tryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {  // 锁未被持有
        // 检查等待队列是否有前驱（公平性）
        if (!hasQueuedPredecessors() &&
            compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);  // 设置持有线程为当前
            return true;
        }
    }
    else if (current == getExclusiveOwnerThread()) {  // 重入
        int nextc = c + acquires;
        setState(nextc);
        return true;
    }
    return false;
}
```

**与 synchronized 对比：**
- **底层实现：** AQS（Java 代码）vs Monitor（JVM 实现）
- **灵活性：** Lock 提供更多功能（公平锁、可中断、超时等）
- **性能：** 竞争激烈时 Lock 略优（可选择是否自旋）

---

## 代码示例

### 10. 如何查看锁升级过程？

**JVM 参数打印锁信息：**
```bash
# 打印锁升级信息
-XX:+PrintBiasedLockingStatistics  # 偏向锁统计
-XX:+TraceBiasedLocking             # 偏向锁跟踪（JDK8 之前）

# 打印 JIT 编译信息
-XX:+PrintCompilation

# JOL 工具查看对象头（Java Object Layout）
java -jar jol-core.jar java.util.HashMap
```

**Java 代码示例：**
```java
package code;

import org.openjdk.jol.info.ClassLayout;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * synchronized 锁升级演示
 */
public class LockUpgradeDemo {
    
    public static void main(String[] args) throws Exception {
        // 需要添加 JOL 依赖才能运行
        // <dependency>
        //     <groupId>org.openjdk.jol</groupId>
        //     <artifactId>jol-core</artifactId>
        //     <version>0.16</version>
        // </dependency>
        
        // 偏向锁演示（需要 JVM 参数：-XX:BiasedLockingStartupDelay=0）
        // demonstrateBiasedLocking();
        
        // 轻量级锁演示
        demonstrateLightweightLocking();
        
        // 重量级锁演示
        demonstrateHeavyweightLocking();
    }
    
    /**
     * 演示偏向锁：单线程反复获取锁
     */
    static void demonstrateBiasedLocking() {
        System.out.println("========== 偏向锁演示 ==========");
        Object lock = new Object();
        
        // 打印对象头（应该是101，偏向锁）
        System.out.println("初始状态：");
        // System.out.println(ClassLayout.parseInstance(lock).toPrintable());
        
        synchronized (lock) {
            System.out.println("第一次获取锁后：");
            // System.out.println(ClassLayout.parseInstance(lock).toPrintable());
        }
        
        synchronized (lock) {
            System.out.println("第二次获取锁后（同一线程，仍是偏向锁）：");
            // System.out.println(ClassLayout.parseInstance(lock).toPrintable());
        }
    }
    
    /**
     * 演示轻量级锁：两个线程交替获取锁（无竞争）
     */
    static void demonstrateLightweightLocking() throws Exception {
        System.out.println("\n========== 轻量级锁演示 ==========");
        
        // 关闭偏向锁，直接进入轻量级锁
        // JVM 参数：-XX:-UseBiasedLocking
        
        Object lock = new Object();
        
        Thread t1 = new Thread(() -> {
            synchronized (lock) {
                System.out.println("线程1获取锁");
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {}
            }
            System.out.println("线程1释放锁");
        });
        
        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(5);  // 确保t1先获取
            } catch (InterruptedException e) {}
            
            synchronized (lock) {
                System.out.println("线程2获取锁");
            }
            System.out.println("线程2释放锁");
        });
        
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        
        System.out.println("轻量级锁演示完成（交替获取，无竞争）");
    }
    
    /**
     * 演示重量级锁：两个线程竞争同一把锁
     */
    static void demonstrateHeavyweightLocking() throws Exception {
        System.out.println("\n========== 重量级锁演示 ==========");
        
        Object lock = new Object();
        
        // t1 长时间持有锁
        Thread t1 = new Thread(() -> {
            synchronized (lock) {
                System.out.println("线程1获取锁，长时间持有...");
                try {
                    Thread.sleep(5000);  // 持有5秒
                } catch (InterruptedException e) {}
            }
            System.out.println("线程1释放锁");
        });
        
        // t2 尝试获取锁，会被阻塞（升级为重量级锁）
        Thread t2 = new Thread(() -> {
            System.out.println("线程2尝试获取锁，将被阻塞...");
            synchronized (lock) {
                System.out.println("线程2终于获取锁（之前被阻塞）");
            }
        });
        
        t1.start();
        Thread.sleep(100);  // 确保t1先获取
        t2.start();
        
        t2.join();
        t1.join();
        
        System.out.println("重量级锁演示完成");
    }
    
    /**
     * synchronized vs Lock 对比
     */
    static void compareSyncAndLock() throws Exception {
        System.out.println("\n========== synchronized vs Lock 对比 ==========");
        
        // synchronized
        Object syncLock = new Object();
        long start1 = System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++) {
            synchronized (syncLock) {
                // 空操作
            }
        }
        System.out.println("synchronized 耗时: " + (System.currentTimeMillis() - start1) + "ms");
        
        // Lock
        Lock lock = new ReentrantLock();
        long start2 = System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++) {
            lock.lock();
            try {
                // 空操作
            } finally {
                lock.unlock();
            }
        }
        System.out.println("Lock 耗时: " + (System.currentTimeMillis() - start2) + "ms");
    }
}

/**
 * synchronized 锁机制总结：
 * 
 * 【锁的四种状态】
 * 1. 无锁（New）：对象创建后
 * 2. 偏向锁（Biased）：单线程访问，Mark Word存储线程ID
 * 3. 轻量级锁（Lightweight）：有竞争但轻，CAS操作，自旋等待
 * 4. 重量级锁（Heavyweight）：竞争激烈，使用Monitor，线程阻塞
 * 
 * 【升级过程】
 * 无锁 → 偏向锁 → 轻量级锁 → 重量级锁
 * （不可逆，除偏向锁可撤销）
 * 
 * 【优化技术】
 * 1. 偏向锁：消除无竞争时的CAS
 * 2. 轻量级锁：避免重量级锁的开销
 * 3. 自适应自旋：根据成功率调整自旋次数
 * 4. 锁消除：逃逸分析后消除无用锁
 * 5. 锁粗化：合并连续的同步块
 * 
 * 【使用建议】
 * 1. 优先使用 synchronized（JDK6+ 优化很好）
 * 2. 需要高级功能再用 Lock（可中断、超时、公平锁等）
 * 3. 减少锁的持有时间
 * 4. 减小锁的粒度
 * 5. 读多写少考虑读写锁（ReadWriteLock）
 */
```

---

## 面试技巧

### 高频面试题 TOP 10

1. **synchronized 和 Lock 的区别？**
2. **synchronized 的锁升级过程是怎样的？**
3. **偏向锁是什么？什么时候会升级为轻量级锁？**
4. **轻量级锁的原理？CAS 失败后会怎样？**
5. **重量级锁为什么开销大？Monitor 的结构是什么？**
6. **锁消除和锁粗化是什么？**
7. **为什么 JDK6 要引入锁升级机制？**
8. **对象头的 Mark Word 存储了哪些信息？**
9. **synchronized 在字节码层面如何实现的？**
10. **ReentrantLock 的实现原理？和 synchronized 对比？**

### 答题模板

**问：请详细讲解 synchronized 的锁升级过程？**

**答：**

> synchronized 在 JDK6 引入了锁升级机制，锁的状态存储在对象头的 Mark Word 中。升级过程是从**无锁 → 偏向锁 → 轻量级锁 → 重量级锁**，只能升级不能降级（除偏向锁可撤销）。
>
> **偏向锁：** 当只有一个线程访问同步块时，会将 Mark Word 的线程ID设为当前线程，标志位设为101。下次同一线程进入时直接检查线程ID，匹配则无需任何CAS操作，性能极高。当其他线程尝试获取时，会撤销偏向并升级为轻量级锁。
>
> **轻量级锁：** 当有轻微竞争时，线程在栈帧中创建 Lock Record，通过 CAS 将对象头的 Mark Word 替换为指向 Lock Record 的指针（标志位00）。如果 CAS 失败，线程会自旋等待。自旋超过阈值（默认10次，自适应调整）仍未获得锁，则升级为重量级锁。
>
> **重量级锁：** 竞争激烈时使用 Monitor（监视器锁），线程会阻塞（park），涉及用户态和内核态切换，开销很大。通过对象头的 Mark Word 找到 Monitor，Owner 指向持有锁的线程，EntryList 存放等待的线程。
>
> 此外，JVM 还有锁消除（逃逸分析后消除无用锁）和锁粗化（合并连续同步块）等优化技术。

---

*点击查看相关代码示例：[LockUpgradeDemo.java](../code/LockUpgradeDemo.java)*

*本文档持续更新，建议收藏备用*