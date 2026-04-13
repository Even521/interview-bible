package code;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Java 并发包常见类演示
 *
 * 1. CountDownLatch、CyclicBarrier、Semaphore、Exchanger
 * 2. Future vs CompletableFuture 核心区别
 * 3. allOf/anyOf 实现原理
 * 4. ForkJoinPool 分治思想
 * 5. 线程池参数设计思路
 *
 * @author Java面试宝典
 */
public class FutureCompletableFutureDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("Java 并发包常见类演示");
        System.out.println("========================================\n");

        // 1. CountDownLatch 倒计时门闩
        demonstrateCountDownLatch();

        // 2. CyclicBarrier 循环栅栏
        demonstrateCyclicBarrier();

        // 3. Semaphore 信号量
        demonstrateSemaphore();

        // 4. Exchanger 交换器
        demonstrateExchanger();

        // 5. Future vs CompletableFuture 对比
        demonstrateFutureVsCompletableFuture();

        // 6. allOf/anyOf 实现原理
        demonstrateAllOfAnyOf();

        // 7. ForkJoinPool 分治思想
        demonstrateForkJoinPool();

        // 8. 线程池参数设计思路
        demonstrateThreadPoolDesign();

        System.out.println("\n========================================");
        System.out.println("演示结束！");
        System.out.println("========================================");
    }

    /**
     * 1. CountDownLatch - 倒计时门闩
     * 适用场景：等待多个线程完成，主线程才能继续
     */
    private static void demonstrateCountDownLatch() throws InterruptedException {
        System.out.println("【演示1】CountDownLatch - 倒计时门闩");
        System.out.println("----------------------------------------");
        System.out.println("原理：初始化计数器，每个线程完成后调用 countDown()");
        System.out.println("主线程 await() 等待，直到计数器归零");

        int taskCount = 3;
        CountDownLatch latch = new CountDownLatch(taskCount);
        ExecutorService executor = Executors.newFixedThreadPool(3);

        System.out.println("\n启动 " + taskCount + " 个子任务：");
        for (int i = 1; i <= taskCount; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    System.out.println("  任务 " + taskId + " 执行中...");
                    Thread.sleep(1000);
                    System.out.println("  任务 " + taskId + " 完成！countDown() ");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown(); // 计数减1
                }
            });
        }

        System.out.println("主线程 await() 等待...");
        latch.await(); // 等待计数器归零
        System.out.println("所有任务完成，主线程继续执行");

        executor.shutdown();
        System.out.println();
    }

    /**
     * 2. CyclicBarrier - 循环栅栏
     * 适用场景：多个线程互相等待到达屏障点后才能继续
     */
    private static void demonstrateCyclicBarrier() {
        System.out.println("【演示2】CyclicBarrier - 循环栅栏");
        System.out.println("----------------------------------------");
        System.out.println("原理：指定数量线程调用 await() 后，全部到达屏障才继续");
        System.out.println("可重复使用（循环），支持到达屏障时的回调动作");

        int partyCount = 3;
        CyclicBarrier barrier = new CyclicBarrier(partyCount, () -> {
            System.out.println("  【回调】所有线程到达屏障！继续执行...");
        });

        System.out.println("\n" + partyCount + " 个线程分批次汇集：");
        ExecutorService executor = Executors.newFixedThreadPool(3);

        for (int round = 1; round <= 2; round++) { // 演示循环使用
            System.out.println("\n第 " + round + " 批线程：");
            for (int i = 1; i <= partyCount; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        System.out.println("  线程 " + threadId + " 执行任务...");
                        Thread.sleep((long) (Math.random() * 1000));
                        System.out.println("  线程 " + threadId + " 到达屏障 await()");
                        barrier.await(); // 等待其他线程
                        System.out.println("  线程 " + threadId + " 继续执行");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // 等待本轮完成
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
        System.out.println("\n【区别】CountDownLatch 一次性，CyclicBarrier 可循环");
        System.out.println();
    }

    /**
     * 3. Semaphore - 信号量
     * 适用场景：控制并发访问的线程数量（限流、资源池）
     */
    private static void demonstrateSemaphore() throws InterruptedException {
        System.out.println("【演示3】Semaphore - 信号量（限流）");
        System.out.println("----------------------------------------");
        System.out.println("原理：维护许可证数量，acquire() 获取, release() 释放");
        System.out.println("适用场景：数据库连接池、API限流、并发控制");

        // 只有2个许可证，即最多2个线程并发执行
        Semaphore semaphore = new Semaphore(2);
        ExecutorService executor = Executors.newFixedThreadPool(5);

        System.out.println("\n启动5个任务，但只允许2个并发：");
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    System.out.println("  任务 " + taskId + " 等待获取许可证...");
                    semaphore.acquire(); // 获取许可

                    System.out.println("  【任务 " + taskId + "】获得许可，开始执行（并发中）");
                    Thread.sleep(1000); // 模拟耗时操作

                    System.out.println("  【任务 " + taskId + "】执行完毕，释放许可");
                    semaphore.release(); // 释放许可
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println();
    }

    /**
     * 4. Exchanger - 交换器
     * 适用场景：两个线程间交换数据
     */
    private static void demonstrateExchanger() {
        System.out.println("【演示4】Exchanger - 线程间数据交换");
        System.out.println("----------------------------------------");
        System.out.println("原理：两个线程调用 exchange() 方法在此点对换数据");
        System.out.println("适用场景：遗传算法、管道设计等两两匹配场景");

        Exchanger<String> exchanger = new Exchanger<>();

        Thread producer = new Thread(() -> {
            try {
                String data = "生产者的数据";
                System.out.println("生产者准备数据: " + data);
                String received = exchanger.exchange(data);
                System.out.println("生产者收到: " + received);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread consumer = new Thread(() -> {
            try {
                Thread.sleep(500); // 模拟处理时间差异
                String data = "消费者的数据";
                System.out.println("消费者准备数据: " + data);
                String received = exchanger.exchange(data);
                System.out.println("消费者收到: " + received);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        System.out.println("\n启动两个线程交换数据：");
        producer.start();
        consumer.start();

        try {
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println();
    }

    /**
     * 5. Future vs CompletableFuture 核心区别
     */
    private static void demonstrateFutureVsCompletableFuture() throws Exception {
        System.out.println("【演示5】Future vs CompletableFuture 核心区别");
        System.out.println("----------------------------------------");

        System.out.println("\n【Future 的局限性】");
        System.out.println("1. 不支持手动完成（不能主动设置结果）");
        System.out.println("2. 不能组合多个 Future（不能链式调用）");
        System.out.println("3. 没有异常处理机制");
        System.out.println("4. get() 会阻塞线程");

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Future 基本用法
        Future<String> future = executor.submit(() -> {
            Thread.sleep(100);
            return "Future 结果";
        });

        try {
            String result = future.get(); // 阻塞等待
            System.out.println("Future get(): " + result);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        System.out.println("\n【CompletableFuture 的优势】");
        System.out.println("1. 支持手动完成 complete(T)");
        System.out.println("2. 支持组合 thenCombine, thenCompose");
        System.out.println("3. 支持异常处理 exceptionally, handle");
        System.out.println("4. 支持异步执行 supplyAsync, thenApplyAsync");

        // 1. 基本创建
        CompletableFuture<String> cf1 = CompletableFuture.completedFuture("直接完成");

        // 2. 异步执行
        CompletableFuture<String> cf2 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "supplyAsync 结果";
        });

        // 3. 链式处理
        cf2.thenApply(result -> result + " -> thenApply 处理")
                .thenAccept(System.out::println);

        // 4. 组合两个 CompletableFuture
        CompletableFuture<String> cfA = CompletableFuture.supplyAsync(() -> {
            sleep(100);
            return "结果A";
        });

        CompletableFuture<String> cfB = CompletableFuture.supplyAsync(() -> {
            sleep(150);
            return "结果B";
        });

        // thenCombine: 两个都完成后合并
        CompletableFuture<String> combined = cfA.thenCombine(cfB, (a, b) -> a + " + " + b);
        System.out.println("thenCombine: " + combined.get());

        // 5. 异常处理
        CompletableFuture<String> withException = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("故意抛异常");
        }).exceptionally(ex -> {
            System.out.println("捕获异常: " + ex.getMessage());
            return "默认值";
        });

        System.out.println("异常处理后: " + withException.get());

        // 6. thenCompose (链式，避免嵌套)
        CompletableFuture<String> composed = CompletableFuture.supplyAsync(() -> "Hello")
                .thenCompose(s -> CompletableFuture.supplyAsync(() -> s + " World"));
        System.out.println("thenCompose: " + composed.get());

        executor.shutdown();
        System.out.println();
    }

    /**
     * 6. allOf/anyOf 实现原理
     */
    private static void demonstrateAllOfAnyOf() throws Exception {
        System.out.println("【演示6】allOf/anyOf 实现原理与使用");
        System.out.println("----------------------------------------");

        System.out.println("【核心原理】");
        System.out.println("allOf: 等待所有 CompletableFuture 完成");
        System.out.println("  实现：使用 CountDownLatch 或二叉树合并");
        System.out.println("  返回：CompletableFuture<Void>（无返回值）");

        System.out.println("anyOf: 任意一个完成即返回");
        System.out.println("  实现：使用 AtomicReference + 回调，第一个完成的设置结果");
        System.out.println("  返回：CompletableFuture<Object>（最快的那个结果）");

        // allOf 示例
        System.out.println("\n【allOf 示例 - 等待所有任务完成】");
        CompletableFuture<String> task1 = CompletableFuture.supplyAsync(() -> {
            sleep(500);
            return "任务1";
        });
        CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> {
            sleep(300);
            return "任务2";
        });
        CompletableFuture<String> task3 = CompletableFuture.supplyAsync(() -> {
            sleep(700);
            return "任务3";
        });

        long start = System.currentTimeMillis();
        CompletableFuture<Void> all = CompletableFuture.allOf(task1, task2, task3);
        all.join(); // 等待所有完成
        System.out.println("allOf 等待时间: " + (System.currentTimeMillis() - start) + "ms");

        // 获取所有结果
        List<String> results = Arrays.asList(task1, task2, task3).stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        System.out.println("所有结果: " + results);

        // anyOf 示例
        System.out.println("\n【anyOf 示例 - 取最快完成的任务】");
        CompletableFuture<Object> any = CompletableFuture.anyOf(
                CompletableFuture.supplyAsync(() -> {
                    sleep(300);
                    return "快的任务";
                }),
                CompletableFuture.supplyAsync(() -> {
                    sleep(500);
                    return "慢的任务";
                })
        );

        System.out.println("anyOf 结果: " + any.get());

        // 实际应用：并行查询多个服务，取最快返回的
        System.out.println("\n【应用场景】并行查询多个服务");
        CompletableFuture<String> result = queryFastestService();
        System.out.println("最快返回的结果: " + result.get());

        System.out.println();
    }

    /**
     * 并行查询多个服务，取最快返回的
     */
    private static CompletableFuture<String> queryFastestService() {
        CompletableFuture<String> serviceA = CompletableFuture.supplyAsync(() -> {
            sleep(200);
            return "服务A结果";
        });
        CompletableFuture<String> serviceB = CompletableFuture.supplyAsync(() -> {
            sleep(150);
            return "服务B结果";
        });
        CompletableFuture<String> serviceC = CompletableFuture.supplyAsync(() -> {
            sleep(300);
            return "服务C结果";
        });

        return CompletableFuture.anyOf(serviceA, serviceB, serviceC)
                .thenApply(result -> (String) result);
    }

    /**
     * 7. ForkJoinPool 分治思想
     */
    private static void demonstrateForkJoinPool() {
        System.out.println("【演示7】ForkJoinPool 分治思想");
        System.out.println("----------------------------------------");

        System.out.println("【核心思想】");
        System.out.println("分治法 (Divide and Conquer):");
        System.out.println("1. Fork: 将大任务拆分成小任务");
        System.out.println("2. Join: 合并子任务的结果");
        System.out.println("3. 工作窃取 (Work Stealing): 空闲线程窃取其他线程的任务");

        // 创建 ForkJoinPool
        ForkJoinPool forkJoinPool = new ForkJoinPool();

        // 使用 RecursiveTask（有返回值）
        System.out.println("\n【大任务：计算 1 到 100 的和】");
        ForkJoinSumTask task = new ForkJoinSumTask(1, 100);
        int result = forkJoinPool.invoke(task);
        System.out.println("1+2+...+100 = " + result);

        // 使用 RecursiveAction（无返回值）
        System.out.println("\n【并行处理数组】");
        int[] array = IntStream.range(1, 11).toArray();
        ForkJoinActionTask action = new ForkJoinActionTask(array, 0, array.length);
        forkJoinPool.invoke(action);
        System.out.println("处理后数组: " + Arrays.toString(array));

        // Java 8+ Stream 并行流底层使用 ForkJoinPool
        System.out.println("\n【并行流 Stream.parallel()】");
        long sum = IntStream.range(1, 1000)
                .parallel() // 自动使用 ForkJoinPool.commonPool()
                .sum();
        System.out.println("并行流求和: " + sum);

        forkJoinPool.shutdown();
        System.out.println();
    }

    /**
     * 递归求和任务（RecursiveTask 有返回值）
     */
    static class ForkJoinSumTask extends RecursiveTask<Integer> {
        private final int start;
        private final int end;
        private static final int THRESHOLD = 20; // 阈值，小于则直接计算

        ForkJoinSumTask(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        protected Integer compute() {
            // 小任务直接计算
            if (end - start <= THRESHOLD) {
                int sum = 0;
                for (int i = start; i < end; i++) {
                    sum += i;
                }
                System.out.println("  计算区间 [" + start + ", " + end + ") = " + sum);
                return sum;
            }

            // 大任务拆分
            int middle = (start + end) / 2;
            ForkJoinSumTask leftTask = new ForkJoinSumTask(start, middle);
            ForkJoinSumTask rightTask = new ForkJoinSumTask(middle, end);

            // 异步执行子任务
            leftTask.fork();
            rightTask.fork();

            // 等待并合并结果
            return leftTask.join() + rightTask.join();
        }
    }

    /**
     * 递归处理任务（RecursiveAction 无返回值）
     */
    static class ForkJoinActionTask extends RecursiveAction {
        private final int[] array;
        private final int start;
        private final int end;
        private static final int THRESHOLD = 3;

        ForkJoinActionTask(int[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            if (end - start <= THRESHOLD) {
                // 小任务：每个元素乘以2
                for (int i = start; i < end; i++) {
                    array[i] *= 2;
                }
                System.out.println("  处理区间 [" + start + ", " + end + ")");
            } else {
                int middle = (start + end) / 2;
                ForkJoinActionTask left = new ForkJoinActionTask(array, start, middle);
                ForkJoinActionTask right = new ForkJoinActionTask(array, middle, end);

                // invokeAll 自动拆分和等待
                invokeAll(left, right);
            }
        }
    }

    /**
     * 8. 线程池参数设计思路
     */
    private static void demonstrateThreadPoolDesign() {
        System.out.println("【演示8】线程池参数设计思路");
        System.out.println("----------------------------------------");

        System.out.println("【核心参数】");
        System.out.println("1. corePoolSize: 核心线程数");
        System.out.println("   - CPU密集型任务: core = NCPU + 1");
        System.out.println("   - IO密集型任务: core = NCPU * 2");
        System.out.println("   - 混合任务: core = NCPU * (1 + waitTime/computeTime)");

        System.out.println("\n2. maximumPoolSize: 最大线程数");
        System.out.println("   - 通常设置为 corePoolSize 的 2~4 倍");
        System.out.println("   - 防止无限制创建线程导致资源耗尽");

        System.out.println("\n3. keepAliveTime: 非核心线程存活时间");
        System.out.println("   - 超过这个时间没任务则回收非核心线程");
        System.out.println("   - allowCoreThreadTimeOut(true) 允许核心线程也被回收");

        System.out.println("\n4. workQueue: 任务队列");
        System.out.println("   - ArrayBlockingQueue: 有界队列（推荐）");
        System.out.println("   - LinkedBlockingQueue: 无界队列（可能OOM）");
        System.out.println("   - SynchronousQueue: 直接交
