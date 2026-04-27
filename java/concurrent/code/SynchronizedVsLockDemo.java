import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.*;

/**
 * synchronized vs Lock(ReentrantLock) 对比演示
 *
 * 核心区别：
 * 1. 实现方式：synchronized 是 JVM 层面(监视器锁)，Lock 是 API 层面(AQS)
 * 2. 灵活性：Lock 需要手动获取/释放，必须在 finally 中释放
 * 3. 功能扩展：Lock 支持可中断、超时获取、公平锁、多条件变量
 * 4. 性能：JDK6+ 后 synchronized 优化很好，竞争激烈时 Lock 略优
 *
 * @author Java面试宝典
 */
public class SynchronizedVsLockDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("synchronized vs Lock 对比演示");
        System.out.println("========================================\n");

        // 1. synchronized 基本用法
        demonstrateSynchronized();

        // 2. ReentrantLock 基本用法
        demonstrateReentrantLock();

        // 3. Lock 的高级特性
        demonstrateAdvancedLockFeatures();

        // 4. 读写锁演示
        demonstrateReadWriteLock();

        // 5. Condition 条件变量
        demonstrateCondition();

        // 6. 性能对比
        demonstratePerformanceComparison();

        System.out.println("\n========================================");
        System.out.println("演示结束！");
        System.out.println("========================================");
    }

    /**
     * 演示 synchronized 的基本用法
     *
     * synchronized 三种用法：
     * 1. 同步实例方法：锁对象是当前实例(this)
     * 2. 同步静态方法：锁对象是类的 Class 对象
     * 3. 同步代码块：可以指定任意对象作为锁
     */
    private static void demonstrateSynchronized() throws InterruptedException {
        System.out.println("【演示1】synchronized 基本用法");
        System.out.println("----------------------------------------");

        SynchronizedResource resource = new SynchronizedResource();

        // 线程1：调用同步方法
        Thread t1 = new Thread(
            () -> {
                resource.synchronizedMethod("Thread-A");
            },
            "Thread-A"
        );

        // 线程2：调用同步代码块
        Thread t2 = new Thread(
            () -> {
                resource.synchronizedBlock("Thread-B");
            },
            "Thread-B"
        );

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("synchronized 特点：");
        System.out.println(" - JVM 自动管理锁的获取和释放");
        System.out.println(" - 代码简洁，无需手动释放");
        System.out.println(" - 不可中断，不能设置超时");
        System.out.println(" - 非公平锁");
        System.out.println();
    }

    /**
     * 演示 ReentrantLock 的基本用法
     *
     * 必须注意：必须在 finally 中释放锁！
     */
    private static void demonstrateReentrantLock() throws InterruptedException {
        System.out.println("【演示2】ReentrantLock 基本用法");
        System.out.println("----------------------------------------");

        LockResource resource = new LockResource();

        Thread t1 = new Thread(
            () -> {
                resource.lockMethod("Thread-A");
            },
            "Thread-A"
        );

        Thread t2 = new Thread(
            () -> {
                resource.lockMethod("Thread-B");
            },
            "Thread-B"
        );

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("ReentrantLock 特点：");
        System.out.println(" - 需要手动 lock() 和 unlock()");
        System.out.println(" - 必须在 finally 中 unlock()，否则可能死锁！");
        System.out.println(" - 可重入（同一线程可多次获取）");
        System.out.println();
    }

    /**
     * 演示 Lock 的高级特性
     * 1. tryLock(): 尝试获取锁，立即返回 boolean
     * 2. tryLock(long, TimeUnit): 尝试获取锁，带超时
     * 3. lockInterruptibly(): 可响应中断的锁获取
     * 4. 公平锁
     */
    private static void demonstrateAdvancedLockFeatures()
        throws InterruptedException {
        System.out.println("【演示3】Lock 高级特性（可中断、超时、公平锁）");
        System.out.println("----------------------------------------");

        // 1. tryLock() 非阻塞获取
        Lock nonBlockingLock = new ReentrantLock();
        System.out.println("1. tryLock() - 非阻塞获取：");
        boolean acquired = nonBlockingLock.tryLock();
        System.out.println("   获取锁成功: " + acquired);
        if (acquired) {
            try {
                System.out.println("   执行临界区代码");
            } finally {
                nonBlockingLock.unlock();
            }
        }

        // 2. tryLock(long, TimeUnit) 带超时
        Lock timeoutLock = new ReentrantLock();
        System.out.println("\n2. tryLock(3, SECONDS) - 带超时获取：");
        Thread t1 = new Thread(() -> {
            timeoutLock.lock();
            System.out.println("   线程1获取锁，持有5秒...");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            } finally {
                timeoutLock.unlock();
                System.out.println("   线程1释放锁");
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(5); // 确保t1先获取
            } catch (InterruptedException e) {}

            try {
                System.out.println("   线程2尝试获取锁，最多等待3秒...");
                if (timeoutLock.tryLock(3, TimeUnit.SECONDS)) {
                    try {
                        System.out.println("   线程2获取锁成功");
                    } finally {
                        timeoutLock.unlock();
                    }
                } else {
                    System.out.println("   线程2获取锁超时，放弃");
                }
            } catch (InterruptedException e) {
                System.out.println("   线程2被中断");
            }
        });

        t1.start();
        Thread.sleep(100); // 确保t1先获取
        t2.start();
        t1.join();
        t2.join();

        // 3. lockInterruptibly() 可中断
        System.out.println("\n3. lockInterruptibly() - 可中断获取：");
        Lock interruptibleLock = new ReentrantLock();
        Thread t3 = new Thread(() -> {
            interruptibleLock.lock();
            System.out.println("   线程3获取锁，持有...");
            try {
                Thread.sleep(10000); // 持有10秒
            } catch (InterruptedException e) {
                System.out.println("   线程3睡眠被中断");
            } finally {
                interruptibleLock.unlock();
            }
        });

        Thread t4 = new Thread(() -> {
            try {
                System.out.println("   线程4 lockInterruptibly() 等待锁...");
                interruptibleLock.lockInterruptibly();
                try {
                    System.out.println("   线程4获取锁成功");
                } finally {
                    interruptibleLock.unlock();
                }
            } catch (InterruptedException e) {
                System.out.println("   线程4获取锁过程中被中断");
            }
        });

        t3.start();
        Thread.sleep(100);
        t4.start();
        Thread.sleep(2000);
        t4.interrupt(); // 中断线程4的等待

        t3.join();

        // 4. 公平锁
        System.out.println("\n4. 公平锁 vs 非公平锁：");
        System.out.println(
            "   公平锁：按照请求锁的顺序获取（new ReentrantLock(true)）"
        );
        System.out.println("   非公平锁：允许插队（默认，效率高）");
        System.out.println();
    }

    /**
     * 演示 ReadWriteLock 读写锁
     *
     * 特点：
     * - 读读不互斥（多个线程可同时读）
     * - 读写互斥
     * - 写写互斥
     *
     * 适用于读多写少的场景，提高并发性
     */
    private static void demonstrateReadWriteLock() throws InterruptedException {
        System.out.println("【演示4】ReadWriteLock 读写锁");
        System.out.println("----------------------------------------");

        ReadWriteLockResource resource = new ReadWriteLockResource();

        // 3个读线程
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);

        System.out.println("启动3个读线程和2个写线程：");

        for (int i = 0; i < 3; i++) {
            final int readerId = i;
            executor.submit(() -> {
                resource.read("Reader-" + readerId);
                latch.countDown();
            });
        }

        Thread.sleep(100); // 让读线程先获取

        for (int i = 0; i < 2; i++) {
            final int writerId = i;
            executor.submit(() -> {
                resource.write("Writer-" + writerId, "Value-" + writerId);
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();

        System.out.println("\n读写锁特点：");
        System.out.println(" - 读读共享：多个线程可同时读");
        System.out.println(" - 读写互斥：读时不能写，写时不能读");
        System.out.println(" - 写写互斥：多个线程不能同时写");
        System.out.println();
    }

    /**
     * 演示 Condition 条件变量
     *
     * Condition 优势：
     * - 一个 Lock 可以创建多个 Condition（多个等待队列）
     * - 可以更精确地控制哪些线程被唤醒
     * - 替代 Object 的 wait/notify（wait/notify 只能有一个条件队列）
     */
    private static void demonstrateCondition() throws InterruptedException {
        System.out.println("【演示5】Condition 条件变量");
        System.out.println("----------------------------------------");

        ConditionResource resource = new ConditionResource();

        System.out.println("使用 Condition 实现生产者-消费者（多条件队列）：");

        Thread producer = new Thread(
            () -> {
                for (int i = 0; i < 5; i++) {
                    resource.produce("Item-" + i);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            },
            "Producer"
        );

        Thread consumer = new Thread(
            () -> {
                for (int i = 0; i < 5; i++) {
                    resource.consume();
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            },
            "Consumer"
        );

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();

        System.out.println("\nCondition 优势：");
        System.out.println(" - 一个 Lock 可创建多个 Condition");
        System.out.println(" - 可以精准唤醒特定条件的线程");
        System.out.println(" - 替代 wait/notify，功能更强大");
        System.out.println();
    }

    /**
     * 性能对比测试
     *
     * 注意：JDK6+ 后 synchronized 优化很好，大部分场景与 Lock 性能接近
     * 只有在极端并发场景下 Lock 才略优
     */
    private static void demonstratePerformanceComparison() {
        System.out.println("【演示6】synchronized vs Lock 性能对比");
        System.out.println("----------------------------------------");

        final int count = 1000000;
        final int threads = 8;

        // 测试 synchronized
        Object syncLock = new Object();
        long start1 = System.currentTimeMillis();

        ExecutorService executor1 = Executors.newFixedThreadPool(threads);
        CountDownLatch latch1 = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor1.submit(() -> {
                for (int j = 0; j < count / threads; j++) {
                    synchronized (syncLock) {
                        // 简单操作
                    }
                }
                latch1.countDown();
            });
        }

        try {
            latch1.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long time1 = System.currentTimeMillis() - start1;
        executor1.shutdown();

        // 测试 ReentrantLock
        Lock reentrantLock = new ReentrantLock();
        long start2 = System.currentTimeMillis();

        ExecutorService executor2 = Executors.newFixedThreadPool(threads);
        CountDownLatch latch2 = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor2.submit(() -> {
                for (int j = 0; j < count / threads; j++) {
                    reentrantLock.lock();
                    try {
                        // 简单操作
                    } finally {
                        reentrantLock.unlock();
                    }
                }
                latch2.countDown();
            });
        }

        try {
            latch2.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long time2 = System.currentTimeMillis() - start2;
        executor2.shutdown();

        System.out.println(
            "线程数: " +
                threads +
                ", 每个线程操作: " +
                (count / threads) +
                " 次"
        );
        System.out.println("synchronized 耗时: " + time1 + " ms");
        System.out.println("ReentrantLock 耗时: " + time2 + " ms");
        System.out.println(
            (time1 <= time2 ? "synchronized" : "Lock") + " 在本环境下更快"
        );

        System.out.println("\n性能结论：");
        System.out.println(" - JDK6+ synchronized 经过优化，与 Lock 性能接近");
        System.out.println(" - 低竞争：两者性能相当");
        System.out.println(" - 高竞争：Lock 略优（可选择策略）");
        System.out.println(
            " - 无竞争：synchronized 有锁升级优势（偏向锁->轻量级锁）"
        );
        System.out.println();
    }

    // ==================== 辅助类 ====================

    /**
     * synchronized 资源类
     */
    static class SynchronizedResource {

        private int count = 0;

        // 同步方法（锁是 this）
        public synchronized void synchronizedMethod(String threadName) {
            System.out.println("   " + threadName + " 进入同步方法");
            count++;
            System.out.println(
                "   " + threadName + " count=" + count + "，即将离开同步方法"
            );
        }

        // 同步代码块（可指定锁对象）
        public void synchronizedBlock(String threadName) {
            synchronized (this) {
                System.out.println("   " + threadName + " 进入同步代码块");
                count++;
                System.out.println(
                    "   " +
                        threadName +
                        " count=" +
                        count +
                        "，即将离开同步代码块"
                );
            }
        }
    }

    /**
     * Lock 资源类
     */
    static class LockResource {

        private final Lock lock = new ReentrantLock();
        private int count = 0;

        public void lockMethod(String threadName) {
            // 获取锁
            lock.lock();
            try {
                System.out.println("   " + threadName + " 获取 Lock");
                count++;
                System.out.println("   " + threadName + " count=" + count);
            } finally {
                // 必须在 finally 中释放！
                lock.unlock();
                System.out.println("   " + threadName + " 释放 Lock");
            }
        }
    }

    /**
     * 读写锁资源类
     */
    static class ReadWriteLockResource {

        private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
        private final Lock readLock = rwLock.readLock();
        private final Lock writeLock = rwLock.writeLock();
        private volatile String data = "初始值";

        public void read(String readerName) {
            readLock.lock();
            try {
                System.out.println(
                    "   " + readerName + " 获取读锁，读取数据: " + data
                );
                Thread.sleep(500); // 模拟读取耗时
                System.out.println("   " + readerName + " 释放读锁");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                readLock.unlock();
            }
        }

        public void write(String writerName, String newValue) {
            writeLock.lock();
            try {
                System.out.println(
                    "   " + writerName + " 获取写锁，准备写入: " + newValue
                );
                Thread.sleep(1000); // 模拟写入耗时
                data = newValue;
                System.out.println("   " + writerName + " 写入完成，释放写锁");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                writeLock.unlock();
            }
        }
    }

    /**
     * Condition 资源类（生产者-消费者模式）
     */
    static class ConditionResource {

        private final Lock lock = new ReentrantLock();
        private final Condition notFull = lock.newCondition(); // 队列不满条件
        private final Condition notEmpty = lock.newCondition(); // 队列不空条件
        private final Queue<String> queue = new LinkedList<>();
        private final int capacity = 3;

        public void produce(String item) {
            lock.lock();
            try {
                // 队列满则等待
                while (queue.size() == capacity) {
                    System.out.println("   队列满，生产者等待");
                    notFull.await();
                }
                queue.offer(item);
                System.out.println(
                    "   生产: " + item + "，队列大小: " + queue.size()
                );
                // 通知消费者可以消费
                notEmpty.signal();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }

        public String consume() {
            lock.lock();
            try {
                // 队列空则等待
                while (queue.isEmpty()) {
                    System.out.println("   队列空，消费者等待");
                    notEmpty.await();
                }
                String item = queue.poll();
                System.out.println(
                    "   消费: " + item + "，队列大小: " + queue.size()
                );
                // 通知生产者可以继续生产
                notFull.signal();
                return item;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            } finally {
                lock.unlock();
            }
        }
    }
}

/**
 * synchronized vs Lock 对比总结
 *
 * 【关键区别】
 * 1. 实现层面：
 *    - synchronized: JVM 层面（字节码指令 monitorenter/monitorexit）
 *    - Lock: API 层面（Java 代码，基于 AQS）
 *
 * 2. 使用方式：
 *    - synchronized: 自动获取/释放，代码简洁
 *    - Lock: 手动 lock/unlock，必须在 finally 中释放
 *
 * 3. 功能特性：
 *    - synchronized: 不可中断，不能超时，非公平，单一条件
 *    - Lock: 可中断，可超时，可公平，多条件（Condition）
 *
 * 4. 锁升级：
 *    - synchronized: 支持（无锁->偏向锁->轻量级锁->重量级锁）
 *    - Lock: 不支持（直接就是 AQS 的 CLH 队列）
 *
 * 5. 场景选择：
 *    - 优先使用 synchronized（JDK6+ 优化很好，代码简洁）
 *    - 需要以下功能时使用 Lock：
 *      * 需要可中断的锁获取
 *      * 需要超时获取锁
 *      * 需要公平锁
 *      * 需要读写分离（ReadWriteLock）
 *      * 需要多个条件队列（Condition）
 *
 * 【最佳实践】
 * 1. 优先使用 synchronized（Java 趋势，性能已优化）
 * 2. 复杂场景使用 Lock（需要高级功能时）
 * 3. 读多写少使用 ReadWriteLock
 * 4. 无论哪种方式，都要最小化锁的持有范围
 * 5. 使用 Lock 时，unlock() 必须在 finally 中
 */
