import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java 线程池使用示例
 *
 * 1. Executors 工厂方法（不推荐，有 OOM 风险）
 * 2. ThreadPoolExecutor 手动创建（推荐）
 * 3. 线程池参数和任务提交流程
 * 4. 优雅关闭线程池
 * 5. 拒绝策略演示
 * 6. ScheduledThreadPool 定时任务
 * 7. CompletableFuture 结合线程池
 *
 * @author Java面试宝典
 */
public class ThreadPoolDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("Java 线程池使用示例");
        System.out.println("========================================\n");

        // 1. Executors 工厂方法
        demonstrateExecutorsFactory();

        // 2. ThreadPoolExecutor 手动创建（推荐）
        demonstrateCustomThreadPool();

        // 3. 线程池执行流程演示
        demonstrateExecutionFlow();

        // 4. 拒绝策略演示
        demonstrateRejectionPolicy();

        // 5. 定时任务线程池
        demonstrateScheduledPool();

        // 6. CompletableFuture 结合线程池
        demonstrateCompletableFuture();

        System.out.println("\n========================================");
        System.out.println("所有演示执行完毕！");
        System.out.println("========================================");
    }

    /**
     * 方式1：Executors 工厂方法（了解即可，不推荐生产环境使用）
     */
    private static void demonstrateExecutorsFactory()
        throws InterruptedException {
        System.out.println("【方式1】Executors 工厂方法（不推荐）");
        System.out.println("----------------------------------------");

        // newFixedThreadPool：固定大小线程池，使用无界队列
        ExecutorService fixedPool = Executors.newFixedThreadPool(2);
        System.out.println("创建 FixedThreadPool（大小=2）");

        // newCachedThreadPool：可缓存线程池，线程数无上限
        ExecutorService cachedPool = Executors.newCachedThreadPool();
        System.out.println("创建 CachedThreadPool（线程数无上限）");

        // newSingleThreadExecutor：单线程线程池
        ExecutorService singlePool = Executors.newSingleThreadExecutor();
        System.out.println("创建 SingleThreadExecutor");

        // newScheduledThreadPool：定时任务线程池
        ScheduledExecutorService scheduledPool =
            Executors.newScheduledThreadPool(2);
        System.out.println("创建 ScheduledThreadPool（核心线程=2）");

        // 关闭线程池
        fixedPool.shutdown();
        cachedPool.shutdown();
        singlePool.shutdown();
        scheduledPool.shutdown();

        System.out.println("说明：Executors 方法简化了创建，但都有潜在风险：");
        System.out.println(
            " - Fixed/Single：使用无界 LinkedBlockingQueue，可能堆积大量任务导致 OOM"
        );
        System.out.println(" - Cached：允许创建无限线程，可能耗尽 CPU 和内存");
        System.out.println(" 推荐做法：直接使用 ThreadPoolExecutor 手动创建");
        System.out.println();
    }

    /**
     * 方式2：ThreadPoolExecutor 手动创建（推荐）
     */
    private static void demonstrateCustomThreadPool()
        throws InterruptedException {
        System.out.println("【方式2】ThreadPoolExecutor 手动创建（推荐）");
        System.out.println("----------------------------------------");

        // 创建有界队列，防止 OOM
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(10);

        // 自定义线程工厂
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(
                    r,
                    "custom-pool-" + count.incrementAndGet()
                );
                System.out.println("创建新线程: " + t.getName());
                return t;
            }
        };

        // 拒绝策略：CallerRunsPolicy（由调用线程执行）
        RejectedExecutionHandler rejectHandler =
            new ThreadPoolExecutor.CallerRunsPolicy();

        // 手动创建线程池
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, // 核心线程数
            5, // 最大线程数
            60L, // 空闲线程存活时间
            TimeUnit.SECONDS, // 时间单位
            workQueue, // 有界任务队列（容量10）
            threadFactory, // 线程工厂
            rejectHandler // 拒绝策略
        );

        System.out.println("线程池配置：");
        System.out.println(" 核心线程数: " + 2);
        System.out.println(" 最大线程数: " + 5);
        System.out.println(" 任务队列: ArrayBlockingQueue(10)");
        System.out.println(" 拒绝策略: CallerRunsPolicy");

        // 提交 8 个任务
        System.out.println("\n提交 8 个任务：");
        for (int i = 0; i < 8; i++) {
            final int taskId = i;
            executor.execute(() -> {
                System.out.println(
                    Thread.currentThread().getName() + " 执行任务-" + taskId
                );
                try {
                    Thread.sleep(500); // 模拟任务耗时
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println(
                    Thread.currentThread().getName() + " 完成任务-" + taskId
                );
            });
        }

        // 等待任务完成
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println();
    }

    /**
     * 演示线程池的执行流程
     */
    private static void demonstrateExecutionFlow() throws InterruptedException {
        System.out.println("【演示】线程池执行流程");
        System.out.println("----------------------------------------");

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, // 核心线程数
            3, // 最大线程数
            60L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(2), // 队列容量2
            new ThreadPoolExecutor.DiscardOldestPolicy() // 丢弃最老任务
        );

        System.out.println("配置：核心线程=2，最大线程=3，队列容量=2");
        System.out.println(
            "流程：核心线程(2) -> 队列(2) -> 非核心线程(1) -> 拒绝"
        );
        System.out.println();

        // 提交 10 个任务，观察执行过程
        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            try {
                executor.execute(() -> {
                    System.out.println(
                        " 线程 " +
                            Thread.currentThread().getName() +
                            " 开始执行任务-" +
                            taskId
                    );
                    try {
                        Thread.sleep(1000); // 模拟耗时任务
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.println(
                        " 线程 " +
                            Thread.currentThread().getName() +
                            " 完成任务-" +
                            taskId
                    );
                });
                System.out.println("提交任务-" + taskId);
            } catch (RejectedExecutionException e) {
                System.out.println(
                    "任务-" + taskId + " 被拒绝：" + e.getMessage()
                );
            }

            // 暂停一下，观察线程创建过程
            Thread.sleep(100);
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        System.out.println();
    }

    /**
     * 演示拒绝策略
     */
    private static void demonstrateRejectionPolicy() {
        System.out.println("【演示】4种拒绝策略");
        System.out.println("----------------------------------------");

        RejectedExecutionHandler[] policies = {
            new ThreadPoolExecutor.AbortPolicy(), // 抛出异常
            new ThreadPoolExecutor.CallerRunsPolicy(), // 调用者执行
            new ThreadPoolExecutor.DiscardPolicy(), // 静默丢弃
            new ThreadPoolExecutor.DiscardOldestPolicy(), // 丢弃最老任务
        };

        String[] policyNames = {
            "AbortPolicy（抛异常）",
            "CallerRunsPolicy（调用者执行）",
            "DiscardPolicy（静默丢弃）",
            "DiscardOldestPolicy（丢弃最老）",
        };

        for (int i = 0; i < policies.length; i++) {
            System.out.println("\n策略 " + (i + 1) + ": " + policyNames[i]);

            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1,
                1,
                0,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1),
                policies[i]
            );

            try {
                // 提交 5 个任务（必定触发拒绝）
                for (int j = 1; j <= 5; j++) {
                    final int taskId = j;
                    executor.execute(() -> {
                        System.out.println(" 执行任务-" + taskId);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }
            } catch (RejectedExecutionException e) {
                System.out.println(" 拒绝策略触发：" + e.getMessage());
            }

            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("\n策略说明：");
        System.out.println(" AbortPolicy：默认，直接抛异常，快速失败");
        System.out.println(" CallerRunsPolicy：降低提交速度，提供反馈机制");
        System.out.println(" DiscardPolicy：静默丢弃，可能导致数据丢失");
        System.out.println(" DiscardOldestPolicy：尝试丢弃等待最久的任务");
        System.out.println();
    }

    /**
     * 演示定时任务线程池
     */
    private static void demonstrateScheduledPool() throws InterruptedException {
        System.out.println("【演示】ScheduledThreadPool 定时任务");
        System.out.println("----------------------------------------");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            2
        );

        System.out.println("1. 延迟 2 秒后执行");
        scheduler.schedule(
            () -> {
                System.out.println(
                    " 延迟任务执行，时间：" + System.currentTimeMillis() / 1000
                );
            },
            2,
            TimeUnit.SECONDS
        );

        System.out.println("2. 初始延迟 1 秒，然后每 3 秒执行一次");
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
            () -> {
                System.out.println(
                    " 周期性任务执行，时间：" +
                        System.currentTimeMillis() / 1000
                );
            },
            1,
            3,
            TimeUnit.SECONDS
        );

        // 让任务执行 3 次后取消
        Thread.sleep(10000);
        System.out.println(" 取消周期性任务");
        future.cancel(false);

        System.out.println(
            "3. 使用 scheduleWithFixedDelay（任务执行完后延迟）"
        );
        scheduler.scheduleWithFixedDelay(
            () -> {
                System.out.println(
                    " FixedDelay 任务执行，时间：" +
                        System.currentTimeMillis() / 1000
                );
                try {
                    Thread.sleep(1000); // 模拟任务耗时 1 秒
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            },
            1,
            2,
            TimeUnit.SECONDS
        );

        Thread.sleep(8000);
        scheduler.shutdown();
        scheduler.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println();
    }

    /**
     * 演示 CompletableFuture 结合线程池
     */
    private static void demonstrateCompletableFuture() {
        System.out.println("【演示】CompletableFuture 结合线程池");
        System.out.println("----------------------------------------");

        // 创建自定义线程池
        ExecutorService executor = new ThreadPoolExecutor(
            2,
            4,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10),
            new ThreadFactory() {
                private final AtomicInteger count = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "cf-pool-" + count.incrementAndGet());
                }
            }
        );

        // 使用 CompletableFuture 异步执行任务
        System.out.println("1. 单个异步任务");
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(
            () -> {
                System.out.println(
                    " 任务1在 " + Thread.currentThread().getName() + " 执行"
                );
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "任务1结果";
            },
            executor
        );

        future1.thenAccept(result -> {
            System.out.println(" 收到结果：" + result);
        });

        // 等待任务完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\n2. 组合多个异步任务");
        CompletableFuture<String> taskA = CompletableFuture.supplyAsync(
            () -> {
                System.out.println(
                    " 任务A在 " + Thread.currentThread().getName() + " 执行"
                );
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "A结果";
            },
            executor
        );

        CompletableFuture<String> taskB = CompletableFuture.supplyAsync(
            () -> {
                System.out.println(
                    " 任务B在 " + Thread.currentThread().getName() + " 执行"
                );
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "B结果";
            },
            executor
        );

        // 等待两个任务都完成
        CompletableFuture<String> combined = taskA.thenCombine(
            taskB,
            (resultA, resultB) -> {
                System.out.println(
                    " 组合结果：A=" + resultA + ", B=" + resultB
                );
                return "最终组合结果";
            }
        );

        try {
            String finalResult = combined.get();
            System.out.println(" 最终结果：" + finalResult);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("\n3. 异常处理");
        CompletableFuture<String> errorTask = CompletableFuture.supplyAsync(
            () -> {
                System.out.println(" 异常任务开始");
                throw new RuntimeException("故意抛出异常");
            },
            executor
        );

        errorTask
            .exceptionally(ex -> {
                System.out.println(" 捕获异常：" + ex.getMessage());
                return "默认返回值";
            })
            .thenAccept(result -> {
                System.out.println(" 最终接收：" + result);
            });

        // 等待完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 优雅关闭线程池
        System.out.println("\n4. 优雅关闭线程池");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println(" 超时，强制关闭...");
                executor.shutdownNow();
            } else {
                System.out.println(" 线程池已正常关闭");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        System.out.println();
    }
}

/**
 * 线程池最佳实践总结：
 *
 * 1. 必须使用 ThreadPoolExecutor 手动创建，不要使用 Executors
 * 2. 使用有界队列（ArrayBlockingQueue/LinkedBlockingQueue(capacity)）防止 OOM
 * 3. 核心线程数根据业务类型设置：
 *    - CPU 密集型：核心数 + 1
 *    - IO 密集型：核心数 * 2
 *    - 混合任务：核心数 * (1 + 等待时间/计算时间)
 * 4. 最大线程数建议为核心线程数的 2-4 倍
 * 5. 拒绝策略推荐使用 CallerRunsPolicy 实现削峰填谷
 * 6. 必须优雅关闭线程池（shutdown + awaitTermination）
 * 7. 监控线程池指标：活跃线程数、队列大小、完成任务数等
 * 8. 使用自定义线程工厂设置有意义的线程名称，方便问题排查
 */
