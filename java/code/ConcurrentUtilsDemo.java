package code;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java 并发工具类演示
 *
 * 包含：
 * 1. CountDownLatch - 倒计时门闩
 * 2. CyclicBarrier - 循环栅栏
 * 3. Semaphore - 信号量
 * 4. Exchanger - 交换器
 * 5. Future vs CompletableFuture 对比
 * 6. ForkJoinPool 分治思想
 * 7. 线程池参数设计思路
 */
public class ConcurrentUtilsDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("Java 并发工具类演示");
        System.out.println("========================================\n");

        // 1. CountDownLatch 演示
        demonstrateCountDownLatch();

        // 2. CyclicBarrier 演示
        demonstrateCyclicBarrier();

        // 3. Semaphore 演示
        demonstrateSemaphore();

        // 4. Exchanger 演示
        demonstrateExchanger();

        // 5. Future vs CompletableFuture
        demonstrateFutureComparison();

        // 6. ForkJoinPool 分治
        demonstrateForkJoin();

        // 7. 线程池参数设计
        demonstrateThreadPoolDesign();

        System.out.println("\n========================================");
        System.out.println("演示结束！");
        System.out.println("========================================");
    }

    /**
     * CountDownLatch：倒计时门闩
     */
    private static void demonstrateCountDownLatch()
        throws InterruptedException {
        System.out.println("【演示1】CountDownLatch - 倒计时门闩");
        System.out.println("----------------------------------------");
        System.out.println("使用场景：主线程等待多个子线程完成任务");
        System.out.println("特点：计数器只能倒计时，不能重复使用\n");

        int workerCount = 3;
        CountDownLatch latch = new CountDownLatch(workerCount);

        System.out.println("主线程：启动 " + workerCount + " 个子线程处理任务");

        for (int i = 1; i <= workerCount; i++) {
            final int workerId = i;
            new Thread(
                () -> {
                    try {
                        System.out.println(
                            " Worker-" + workerId + " 开始工作..."
                        );
                        Thread.sleep(workerId * 1000);
                        System.out.println(
                            " Worker-" + workerId + " 完成工作！"
                        );
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                        System.out.println(" 倒计时剩余：" + latch.getCount());
                    }
                },
                "Worker-" + i
            )
                .start();
        }

        System.out.println("\n主线程：等待所有工作线程完成...");
        latch.await();
        System.out.println("主线程：所有工作完成，继续执行！\n");
    }

    /**
     * CyclicBarrier：循环栅栏
     */
    private static void demonstrateCyclicBarrier() throws InterruptedException {
        System.out.println("【演示2】CyclicBarrier - 循环栅栏");
        System.out.println("----------------------------------------");
        System.out.println("使用场景：多线程分阶段任务，汇合后继续");
        System.out.println("特点：可重复使用，支持到达屏障时的回调操作\n");

        int partyCount = 3;
        CyclicBarrier barrier = new CyclicBarrier(partyCount, () -> {
            System.out.println(" >>> 所有线程到达屏障，继续下一阶段！");
        });

        System.out.println("模拟多线程分阶段任务（3个阶段）");

        for (int i = 1; i <= partyCount; i++) {
            final int threadId = i;
            new Thread(
                () -> {
                    try {
                        System.out.println(
                            " Thread-" + threadId + " 执行阶段1"
                        );
                        Thread.sleep(500);
                        barrier.await();

                        System.out.println(
                            " Thread-" + threadId + " 执行阶段2"
                        );
                        Thread.sleep(800);
                        barrier.await();

                        System.out.println(
                            " Thread-" + threadId + " 执行阶段3"
                        );
                        Thread.sleep(600);
                        barrier.await();

                        System.out.println(
                            " Thread-" + threadId + " 全部完成！"
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                "Thread-" + i
            )
                .start();
        }

        Thread.sleep(4000);
        System.out.println("\nCyclicBarrier vs CountDownLatch：");
        System.out.println(" - CountDownLatch：一个等多个（主等子），不可重用");
        System.out.println(" - CyclicBarrier：多个互相等（子等子），可重用\n");
    }

    /**
     * Semaphore：信号量
     */
    private static void demonstrateSemaphore() throws InterruptedException {
        System.out.println("【演示3】Semaphore - 信号量");
        System.out.println("----------------------------------------");
        System.out.println("使用场景：控制并发数量，限流（如数据库连接池）");
        System.out.println("特点：acquire()获取许可，release()释放许可\n");

        int permits = 2;
        Semaphore semaphore = new Semaphore(permits);

        System.out.println(
            "模拟数据库连接池（最多" + permits + "个并发连接）："
        );

        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            new Thread(
                () -> {
                    try {
                        System.out.println(" Task-" + taskId + " 请求连接...");
                        semaphore.acquire();
                        System.out.println(
                            " Task-" +
                                taskId +
                                " 获取连接（剩余许可：" +
                                semaphore.availablePermits() +
                                "）"
                        );
                        Thread.sleep(1500);
                        System.out.println(" Task-" + taskId + " 释放连接");
                        semaphore.release();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                },
                "Task-" + i
            )
                .start();
        }

        Thread.sleep(5000);
        System.out.println("\n【公平模式】Semaphore(true)");
        System.out.println("公平模式：按照请求顺序获取许可（避免饥饿）\n");
    }

    /**
     * Exchanger：交换器
     */
    private static void demonstrateExchanger() throws InterruptedException {
        System.out.println("【演示4】Exchanger - 交换器");
        System.out.println("----------------------------------------");
        System.out.println(
            "使用场景：两个线程之间交换数据（如遗传算法、流水线）"
        );
        System.out.println("特点：只能用于两个线程，同步点\n");

        Exchanger<String> exchanger = new Exchanger<>();

        System.out.println("两个线程交换数据：");

        new Thread(
            () -> {
                try {
                    String data = "线程A的数据";
                    System.out.println(
                        " 线程A：持有 \"" + data + "\"，准备交换"
                    );
                    String exchanged = exchanger.exchange(data);
                    System.out.println(
                        " 线程A：交换后得到 \"" + exchanged + "\""
                    );
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            },
            "Thread-A"
        )
            .start();

        new Thread(
            () -> {
                try {
                    Thread.sleep(1000);
                    String data = "线程B的数据";
                    System.out.println(
                        " 线程B：持有 \"" + data + "\"，准备交换"
                    );
                    String exchanged = exchanger.exchange(data);
                    System.out.println(
                        " 线程B：交换后得到 \"" + exchanged + "\""
                    );
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            },
            "Thread-B"
        )
            .start();

        Thread.sleep(2000);
        System.out.println();
    }

    /**
     * Future vs CompletableFuture 对比
     */
    private static void demonstrateFutureComparison() throws Exception {
        System.out.println("【演示5】Future vs CompletableFuture");
        System.out.println("----------------------------------------");

        System.out.println("【Future（JDK 5）】");
        System.out.println(" - 基本的异步结果获取");
        System.out.println(" - get() 阻塞等待");
        System.out.println(" - 无法链式组合");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Integer> future = executor.submit(() -> {
            Thread.sleep(1000);
            return 42;
        });
        System.out.println(" Future.get() = " + future.get());
        executor.shutdown();

        System.out.println("\n【CompletableFuture（JDK 8）】");
        System.out.println(" - 支持链式组合（thenApply, thenAccept）");
        System.out.println(" - 支持异常处理（exceptionally）");
        System.out.println(" - 支持组合多个 Future（allOf, anyOf）");
        System.out.println(" - 支持异步执行（supplyAsync, runAsync）");

        CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> 10)
            .thenApply(x -> x * 2)
            .thenApply(x -> "Result: " + x);
        System.out.println(" thenApply链式: " + cf.get());

        CompletableFuture<Integer> cfException = CompletableFuture.<
                Integer
            >supplyAsync(() -> {
            throw new RuntimeException("出错");
        }).exceptionally(ex -> {
            System.out.println(" 捕获异常: " + ex.getMessage());
            return 0;
        });
        System.out.println(" exceptionally: " + cfException.get());

        CompletableFuture<Void> all = CompletableFuture.allOf(
            CompletableFuture.supplyAsync(() -> {
                sleep(100);
                return "A";
            }),
            CompletableFuture.supplyAsync(() -> {
                sleep(200);
                return "B";
            }),
            CompletableFuture.supplyAsync(() -> {
                sleep(150);
                return "C";
            })
        );
        System.out.println(" allOf 等待所有完成: " + all.get());

        CompletableFuture<Object> any = CompletableFuture.anyOf(
            CompletableFuture.supplyAsync(() -> {
                sleep(300);
                return "Slow";
            }),
            CompletableFuture.supplyAsync(() -> {
                sleep(50);
                return "Fast";
            })
        );
        System.out.println(" anyOf 最快结果: " + any.get());

        System.out.println("\n【CompletableFuture allOf 实现原理】");
        System.out.println(" - 内部使用 AtomicInteger 计数");
        System.out.println(" - 每个子任务完成时计数器减1");
        System.out.println(" - 计数器为0时触发完成");
        System.out.println();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {}
    }

    /**
     * ForkJoinPool 分治思想
     */
    private static void demonstrateForkJoin() {
        System.out.println("【演示6】ForkJoinPool - 分治思想");
        System.out.println("----------------------------------------");
        System.out.println("核心思想：Fork（拆分）-> Join（合并）");
        System.out.println("工作窃取：空闲线程从其他线程队列窃取任务");
        System.out.println();

        ForkJoinPool pool = new ForkJoinPool();
        SumTask task = new SumTask(1, 100);
        int result = pool.invoke(task);
        System.out.println("1 到 100 的和 = " + result);

        System.out.println("\n【原理说明】");
        System.out.println(" - RecursiveTask<T>: 有返回值");
        System.out.println(" - RecursiveAction: 无返回值");
        System.out.println(" - Fork: 将任务拆分为子任务，递归执行");
        System.out.println(" - Join: 等待子任务完成并合并结果");
        System.out.println(" - 任务足够小时直接计算，避免拆分开销");
        System.out.println();
    }

    /**
     * 分治任务示例
     */
    static class SumTask extends RecursiveTask<Integer> {

        private static final int THRESHOLD = 10;
        private int start;
        private int end;

        SumTask(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        protected Integer compute() {
            if (end - start <= THRESHOLD) {
                int sum = 0;
                for (int i = start; i <= end; i++) {
                    sum += i;
                }
                System.out.println(" 计算[" + start + "," + end + "] = " + sum);
                return sum;
            }

            int mid = (start + end) / 2;
            SumTask leftTask = new SumTask(start, mid);
            SumTask rightTask = new SumTask(mid + 1, end);

            leftTask.fork();
            rightTask.fork();

            return leftTask.join() + rightTask.join();
        }
    }

    /**
     * 线程池参数设计思路
     */
    private static void demonstrateThreadPoolDesign() {
        System.out.println("【演示7】线程池参数设计思路");
        System.out.println("----------------------------------------");

        System.out.println("【核心参数】");
        System.out.println(" corePoolSize: 核心线程数，常驻线程数量");
        System.out.println(" maximumPoolSize: 最大线程数，非核心线程空闲回收");
        System.out.println(" workQueue: 任务等待队列");
        System.out.println(" keepAliveTime: 非核心线程存活时间");
        System.out.println(" handler: 拒绝策略");
        System.out.println();

        System.out.println("【线程池工作流程】");
        System.out.println(" 1. 提交任务，创建核心线程执行任务");
        System.out.println(" 2. 核心线程满，任务进入队列");
        System.out.println(" 3. 队列满且线程数 < 最大，创建非核心线程");
        System.out.println(" 4. 线程数达最大且队列满，执行拒绝策略");
        System.out.println();

        System.out.println("【核心线程数设计】");

        System.out.println("\n1. CPU密集型（计算多，IO少）");
        System.out.println(" 公式：corePoolSize = CPU核心数 + 1");
        System.out.println(" 原因：多线程太大反而因上下文切换降低性能");

        int cpuCores = Runtime.getRuntime().availableProcessors();
        System.out.println(" 当前CPU核心数：" + cpuCores);
        System.out.println(" 推荐核心线程数：" + (cpuCores + 1));

        System.out.println("\n2. IO密集型（IO多，计算少）");
        System.out.println(" 公式：corePoolSize = CPU核心数 * 2");
        System.out.println(" 或：corePoolSize = CPU核心数 / (1 - 阻塞系数)");
        System.out.println(" 原因：IO阻塞时CPU空闲，可增加线程提高利用率");

        System.out.println(" 推荐核心线程数：" + (cpuCores * 2));

        System.out.println("\n3. 混合型");
        System.out.println(
            " 公式：corePoolSize = (IO时间 + CPU时间) / CPU时间 * CPU核心数"
        );
        System.out.println(" 或拆分为两个线程池分别处理");

        System.out.println("\n【队列选择】");

        System.out.println(" ArrayBlockingQueue: 有界队列，防止OOM");
        System.out.println(" - 推荐生产环境使用");
        System.out.println(" - 队列大小根据内存和任务量设置");

        System.out.println("\n LinkedBlockingQueue: 默认无界，谨慎使用");
        System.out.println(" - newFixedThreadPool 使用");
        System.out.println(" - 任务堆积可能导致OOM");

        System.out.println("\n SynchronousQueue: 直接移交，无容量");
        System.out.println(" - newCachedThreadPool 使用");
        System.out.println(" - 适合任务执行快的场景");

        System.out.println("\n【拒绝策略】");

        System.out.println(" AbortPolicy（默认）: 抛出异常，快速失败");
        System.out.println(" CallerRunsPolicy: 调用者执行，提供反馈机制");
        System.out.println(" DiscardPolicy: 静默丢弃，可能导致数据丢失");
        System.out.println(" DiscardOldestPolicy: 丢弃最老任务，然后重试提交");

        System.out.println("\n【最佳实践】");

        System.out.println(
            " 使用 ThreadPoolExecutor 手动创建（不要用 Executors）"
        );
        System.out.println(" 使用有界队列（ArrayBlockingQueue）");
        System.out.println(" 自定义线程工厂（命令规范，便于排查）");
        System.out.println(" 配置拒绝策略（推荐 CallerRunsPolicy）");
        System.out.println(" 监控线程池指标（活跃线程、队列大小）");
        System.out.println(" 优雅关闭（shutdown + awaitTermination）");

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            cpuCores + 1,
            cpuCores * 2,
            60L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1000),
            new ThreadFactory() {
                private AtomicInteger count = new AtomicInteger(1);

                public Thread newThread(Runnable r) {
                    return new Thread(
                        r,
                        "custom-pool-" + count.incrementAndGet()
                    );
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        System.out.println(
            "\n推荐配置示例：核心=" +
                (cpuCores + 1) +
                ", 最大=" +
                (cpuCores * 2) +
                ", 队列=1000"
        );
        executor.shutdown();
    }
}

/**
 * 并发工具类总结
 *
 * 【CountDownLatch】
 * 场景：等待多个子线程完成（如主线程等待初始化完成）
 * 特点：计数器只能减，到达0释放，不可重用
 *
 * 【CyclicBarrier】
 * 场景：多线程分阶段任务（如并行计算，阶段1完成后才能阶段2）
 * 特点：可重用，支持到达屏障回调
 *
 * 【Semaphore】
 * 场景：限流（如数据库连接池控制并发数）
 * 特点： acquire获取许可，release释放，支持公平模式
 *
 * 【Exchanger】
 * 场景：两个线程交换数据（如遗传算法、流水线）
 * 特点：只能两个线程，同步点
 *
 * 【CompletableFuture vs Future】
 * Future: get阻塞，无法链式，无法组合
 * CompletableFuture: 链式(thenApply), 组合(allOf/anyOf), 异常处理
 *
 * 【ForkJoinPool】
 * 思想：分治（Fork拆分，Join合并）
 * 优化：工作窃取，小任务直接计算
 * 场景：递归分治（排序、搜索、聚合）
 *
 * 【线程池参数设计】
 * CPU密集型: CPU+1
 * IO密集型: CPU*2 或 CPU/(1-阻塞系数)
 * 队列: 推荐ArrayBlockingQueue(有界)
 * 拒绝策略: 推荐CallerRunsPolicy
 */
