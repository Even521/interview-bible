package code;

import java.util.concurrent.*;
import java.util.stream.LongStream;

/**
 * ForkJoinPool 分治思想演示
 *
 * 核心知识点：
 * 1. Fork/Join 框架是什么（任务拆分 + 结果合并）
 * 2. RecursiveTask（有返回值）vs RecursiveAction（无返回值）
 * 3. 工作窃取（Work Stealing）机制
 * 4. 分治算法实现（归并排序、斐波那契、数组求和）
 *
 * @author Java面试宝典
 */
public class ForkJoinPoolDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("ForkJoinPool 分治思想演示");
        System.out.println("========================================\n");

        // 1. Fork/Join 基础介绍
        demonstrateForkJoinIntro();

        // 2. RecursiveTask 演示（有返回值）
        demonstrateRecursiveTask();

        // 3. RecursiveAction 演示（无返回值）
        demonstrateRecursiveAction();

        // 4. 工作窃取机制
        demonstrateWorkStealing();

        // 5. 与线程池对比
        demonstrateComparison();

        // 6. Java 8 Stream 并行流底层
        demonstrateParallelStream();

        System.out.println("\n========================================");
        System.out.println("演示结束！");
        System.out.println("========================================");
    }Java 并发包常见类：CountDownLatch、CyclicBarrier、Semaphore、Exchanger。 • Future、CompletableFuture 的区别，allOf/anyOf 实现原理。 • ForkJoinPool 的分治思想。 • 线程池参数（core/max/queue/拒绝策略）设计思路。

    /**
     * Fork/Join 框架介绍
     */
    private static void demonstrateForkJoinIntro() {
        System.out.println("【演示1】Fork/Join 框架介绍");
        System.out.println("----------------------------------------");

        System.out.println("Fork/Join 框架是什么？");
        System.out.println("  Java 7 引入的并行计算框架，核心是“分治”思想");
        System.out.println("  Fork：将大任务拆分成小任务");
        System.out.println("  Join：等待小任务执行完毕，合并结果");
        System.out.println();

        System.out.println("核心组件：");
        System.out.println("  ForkJoinPool：线程池，管理工作线程");
        System.out.println("  ForkJoinTask：任务基类");
        System.out.println("    ├─ RecursiveTask<V>：有返回值的任务");
        System.out.println("    └─ RecursiveAction：无返回值的任务");
        System.out.println();

        System.out.println("分治算法流程：");
        System.out.println("  if (任务足够小) {");
        System.out.println("    直接计算结果");
        System.out.println("  } else {");
        System.out.println("    拆分成若干子任务");
        System.out.println("    递归执行子任务（fork）");
        System.out.println("    等待子任务完成（join）");
        System.out.println("    合并结果");
        System.out.println("  }");
        System.out.println();

        System.out.println("工作窃取（Work Stealing）：");
        System.out.println("  每个工作线程有自己的任务队列（双端队列）");
        System.out.println("  线程完成自己的任务后，会从其他线程队列“窃取”任务");
        System.out.println("  窃取从队列尾部进行，避免与队列所有者的冲突");
        System.out.println("  提高线程利用率，减少空闲等待");
        System.out.println();
    }

    /**
     * RecursiveTask 演示（有返回值）：计算 1+2+...+n
     */
    private static void demonstrateRecursiveTask() {
        System.out.println("【演示2】RecursiveTask 演示（数组求和）");
        System.out.println("----------------------------------------");

        // 创建大数组
        int size = 100000000; // 1亿个数
        long[] numbers = new long[size];
        for (int i = 0; i < size; i++) {
            numbers[i] = i + 1;
        }

        System.out.println("计算 1 到 " + size + " 的和");
        System.out.println("理论值：" + (long) size * (size + 1) / 2);
        System.out.println();

        // 方法1：单线程计算
        long start = System.currentTimeMillis();
        long sum1 = 0;
        for (long num : numbers) {
            sum1 += num;
        }
        long singleThreadTime = System.currentTimeMillis() - start;
        System.out.println("单线程计算：sum=" + sum1 + ", 耗时=" + singleThreadTime + " ms");

        // 方法2：Fork/Join 并行计算
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        SumTask task = new SumTask(numbers, 0, numbers.length);

        start = System.currentTimeMillis();
        long sum2 = forkJoinPool.invoke(task);
        long forkJoinTime = System.currentTimeMillis() - start;
        System.out.println("Fork/Join 计算：sum=" + sum2 + ", 耗时=" + forkJoinTime + " ms");

        // 方法3：Java 8 Stream 并行流
        start = System.currentTimeMillis();
        long sum3 = LongStream.of(numbers).parallel().sum();
        long streamTime = System.currentTimeMillis() - start;
        System.out.println("Parallel Stream：sum=" + sum3 + ", 耗时=" + streamTime + " ms");

        System.out.println("\n【SumTask 源码】");
        System.out.println("class SumTask extends RecursiveTask<Long> {");
        System.out.println("    private static final int THRESHOLD = 10000;");
        System.out.println("    private long[] array;");
        System.out.println("    private int start, end;");
        System.out.println();
        System.out.println("    protected Long compute() {");
        System.out.println("        if (end - start <= THRESHOLD) {");
        System.out.println("            // 任务足够小，直接计算");
        System.out.println("            long sum = 0;");
        System.out.println("            for (int i = start; i < end; i++) sum += array[i];");
        System.out.println("            return sum;");
        System.out.println("        }");
        System.out.println("        // 任务太大，拆分成两个子任务");
        System.out.println("        int mid = (start + end) / 2;");
        System.out.println("        SumTask left = new SumTask(array, start, mid);");
        System.out.println("        SumTask right = new SumTask(array, mid, end);");
        System.out.println("        left.fork();  // 异步执行左任务");
        System.out.println("        right.fork(); // 异步执行右任务");
        System.out.println("        return left.join() + right.join(); // 等待并合并结果");
        System.out.println("    }");
        System.out.println("}");

        forkJoinPool.shutdown();
        System.out.println();
    }

    /**
     * RecursiveAction 演示（无返回值）：归并排序
     */
    private static void demonstrateRecursiveAction() {
        System.out.println("【演示3】RecursiveAction 演示（归并排序）");
        System.out.println("----------------------------------------");

        // 生成随机数组
        int size = 10000000;
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = (int) (Math.random() * 1000000);
        }

        System.out.println("数组大小：" + size);

        // 复制数组用于对比
        int[] array2 = array.clone();

        // 单线程排序
        long start = System.currentTimeMillis();
        Arrays.sort(array);
        long singleThreadTime = System.currentTimeMillis() - start;
        System.out.println("Arrays.sort() 单线程：耗时=" + singleThreadTime + " ms");

        // Fork/Join 并行归并排序
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        MergeSortTask task = new MergeSortTask(array2, 0, array2.length - 1);

        start = System.currentTimeMillis();
        forkJoinPool.invoke(task);
        long forkJoinTime = System.currentTimeMillis() - start;
        System.out.println("Fork/Join 归并排序：耗时=" + forkJoinTime + " ms");

        // 验证结果
        boolean sorted = true;
        for (int i = 1; i < array2.length; i++) {
            if (array2[i - 1] > array2[i]) {
                sorted = false;
                break;
            }
        }
        System.out.println("排序正确：" + sorted);

        forkJoinPool.shutdown();
        System.out.println();
    }

    /**
     * 演示工作窃取机制
     */
    private static void demonstrateWorkStealing() {
        System.out.println("【演示4】工作窃取（Work Stealing）机制");
        System.out.println("----------------------------------------");

        System.out.println("工作窃取原理：");
        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.println("  │          ForkJoinPool (线程池)          │");
        System.out.println("  │  ┌─────────┐  ┌─────────┐  ┌─────────┐ │");
        System.out.println("  │  │ Worker-1│  │ Worker-2│  │ Worker-3│ │");
        System.out.println("  │  │ [大任务]│  │ [小任务]│  │ [完成]  │ │");
        System.out.println("  │  │ [子任务]│  │ [完成]  │  │         │ │");
        System.out.println("  │  │ [子任务]│  │         │  │         │ │");
        System.out.println("  │  └─────────┘  └─────────┘  └─────────┘ │");
        System.out.println("  │      │            │            │      │");
        System.out.println("  │      ▼            │            ▼      │");
        System.out.println("  │  继续执行      窃取任务      窃取任务   │");
        System.out.println("  └─────────────────────────────────────────┘");
        System.out.println();

        System.out.println("工作窃取特点：");
        System.out.println("1. 每个工作线程维护自己的双端任务队列（Deque）");
        System.out.println("2. 线程从队列头部取任务执行（LIFO）");
        System.out.println("3. 自己的队列为空时，从其他线程队列尾部窃取任务（FIFO）");
        System.out.println("4. 窃取从尾部进行，减少与队列所有者的竞争");
        System.out.println("5. 优点：负载均衡，减少线程空闲");

        // 演示任务分配不均时的工作窃取
        ForkJoinPool pool = new ForkJoinPool(4); // 4个工作线程

        System.out.println("\n演示：任务分配不均");
        long start = System.currentTimeMillis();

        // 提交一个不均衡的任务：部分任务重，部分任务轻
        UnbalancedTask task = new UnbalancedTask(1, 100, 1000000);
        pool.invoke(task);

        long time = System.currentTimeMillis() - start;
        System.out.println("不均衡任务完成，耗时：" + time + " ms");
        System.out.println("工作窃取确保忙线程和闲线程之间负载均衡");

        pool.shutdown();
        System.out.println();
    }

    /**
     * 与线程池对比
     */
    private static void demonstrateComparison() {
        System.out.println("【演示5】Fork/Join 与普通线程池对比");
        System.out.println("----------------------------------------");

        System.out.println("ForkJoinPool vs ThreadPoolExecutor：");
        System.out.println();
        System.out.println("┌─────────────────┬──────────────────┬────────────────────┐");
        System.out.println("│ 特性             │ ForkJoinPool     │ ThreadPoolExecutor │");
        System.out.println("├─────────────────┼──────────────────┼────────────────────┤");
        System.out.println("│ 设计目标         │ 分治递归任务      │ 通用并发任务        │");
        System.out.println("│ 任务类型         │ 可拆分的大任务    │ 独立的小任务        │");
        System.out.println("│ 任务队列         │ 每个线程双端队列  │ 全局阻塞队列        │");
        System.out.println("│ 负载均衡         │ 工作窃取          │ 队列共享            │");
        System.out.println("│ 适用场景         │ 递归、树形、分治  │ IO、计算混合        │");
        System.out.println("│ 任务依赖         │ 支持（join）      │ 不支持              │");
        System.out.println("└─────────────────┴──────────────────┴────────────────────┘");
        System.out.println();

        System.out.println("使用建议：");
        System.out.println("• 递归分治任务（归并排序、斐波那契）：ForkJoinPool");
        System.out.println("• 大量独立小任务：ThreadPoolExecutor");
        System.out.println("• IO密集型任务：ThreadPoolExecutor（ForkJoinPool工作线程阻塞会降低效率）");
        System.out.println("• 计算密集型任务：两者都可以，ForkJoinPool对递归任务更优");
        System.out.println();
    }

    /**
     * Java 8 Parallel Stream 底层
     */
    private static void demonstrateParallelStream() {
        System.out.println("【演示6】Java 8 Parallel Stream 底层原理");
        System.out.println("----------------------------------------");

        System.out.println("Parallel Stream 底层使用 ForkJoinPool！");

        int[] numbers = new int[10000000];
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = i + 1;
        }

        System.out.println("\n串行流 vs 并行流对比：");

        // 串行流
        long start = System.currentTimeMillis();
        long sum1 = Arrays.stream(numbers)
                .filter(n -> n % 2 == 0)
                .mapToLong(n -> n * n)
                .sum();
        long sequentialTime = System.currentTimeMillis() - start;
        System.out.println("串行流：sum=" + sum1 + ", 耗时=" + sequentialTime + " ms");

        // 并行流（底层使用 ForkJoinPool.commonPool()）
        start = System.currentTimeMillis();
        long sum2 = Arrays.stream(numbers)
                .parallel()
                .filter(n -> n % 2 == 0)
                .mapToLong(n -> n * n)
                .sum();
        long parallelTime = System.currentTimeMillis() - start;
        System.out.println("并行流：sum=" + sum2 + ", 耗时=" + parallelTime + " ms");

        System.out.println("\n源码追踪：");
        System.out.println("Arrays.stream(numbers).parallel()...");
        System.out.println("  └─ StreamSupport.stream(..., true)");
        System.out.println("    └─ ReferencePipeline.StatelessOp.parallel()");
        System.out.println("      └─ evaluate()");
        System.out.println("        └─ ForkJoinTask.invoke()");
        System.out.println("          └─ ForkJoinPool.commonPool()");

        System.out.println("\nParallel Stream 使用的线程池：");
        ForkJoinPool commonPool = ForkJoinPool.commonPool();
        System.out.println("Common Pool 并行度：" + commonPool.getParallelism());
        System.out.println("Common Pool 线程数：" + Runtime.getRuntime().availableProcessors());
        System.out.println("（通常等于 CPU 核心数）");

        System.out.println("\n自定义并行度：");
        System.out.println("java -Djava.util.concurrent.ForkJoinPool.common.parallelism=8 MyApp");
        System.out.println("或");
        System.out.println("ForkJoinPool customPool = new ForkJoinPool(8);");
        System.out.println("customPool.submit(() ->");
        System.out.println("    list.parallelStream().map(...).collect(...)").join();");
        System.out.println();
    }

    // ==================== 任务类 ====================

    /**
     * 求和任务（RecursiveTask：有返回值）
     */
    static class SumTask extends RecursiveTask<Long> {
        private static final int THRESHOLD = 100000;
        private final long[] array;
        private final int start;
        private final int end;

        SumTask(long[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Long compute() {
            // 如果任务足够小，直接计算
            if (end - start <= THRESHOLD) {
                long sum = 0;
                for (int i = start; i < end; i++) {
                    sum += array[i];
                }
                return sum;
            }

            // 拆分任务
            int mid = (start + end) / 2;
            SumTask left = new SumTask(array, start, mid);
            SumTask right = new SumTask(array, mid, end);

            // 异步执行
            left.fork();
            right.fork();

            // 等待并合并结果
            return left.join() + right.join();

            // 优化写法：invokeAll(left, right); return left.join() + right.join();
        }
    }

    /**
     * 归并排序任务（RecursiveAction：无返回值）
     */
    static class MergeSortTask extends RecursiveAction {
        private static final int THRESHOLD = 10000;
        private final int[] array;
        private final int left;
        private final int right;

        MergeSortTask(int[] array, int left, int right) {
            this.array = array;
            this.left = left;
            this.right = right;
        }

        @Override
        protected void compute() {
            if (right - left <= THRESHOLD) {
                // 小规模用快速排序
                Arrays.sort(array, left, right + 1);
                return;
            }

            int mid = (left + right) / 2;

            MergeSortTask leftTask = new MergeSortTask(array, left, mid);
            MergeSortTask rightTask = new MergeSortTask(array, mid + 1, right);

            invokeAll(leftTask, rightTask);

            // 合并两个有序数组
            merge(left, mid, right);
        }

        private void merge(int left, int mid, int right) {
            int[] temp = new int[right - left + 1];
            int i = left, j = mid + 1, k = 0;

            while (i <= mid && j <= right) {
                if (array[i] <= array[j]) {
                    temp[k++] = array[i++];
                } else {
                    temp[k++] = array[j++];
                }
            }

            while (i <= mid) {
                temp[k++] = array[i++];
            }

            while (j <= right) {
                temp[k++] = array[j++];
            }

            System.arraycopy(temp, 0, array, left, temp.length);
        }
    }

    /**
     * 不均衡任务（演示工作窃取）
     */
    static class UnbalancedTask extends RecursiveTask<Void> {
        private final int start;
        private final int end;
        private final int heavyTaskEnd;

        UnbalancedTask(int start, int end, int heavyTaskEnd) {
            this.start = start;
            this.end = end;
            this.heavyTaskEnd = heavyTaskEnd;
        }

        @Override
        protected Void compute() {
            if (end - start <= 10) {
                // 模拟不同计算量的任务
                if (start <= heavyTaskEnd) {
                    // 重任务
                    heavyComputation();
                } else {
                    // 轻任务
                    lightComputation();
                }
                return null;
            }

            int mid = (start + end) / 2;
            UnbalancedTask left = new UnbalancedTask(start, mid, heavyTaskEnd);
            UnbalancedTask right = new UnbalancedTask(mid, end, heavyTaskEnd);

            invokeAll(left, right);
            return null;
        }

        private void heavyComputation() {
            // 模拟计算密集型任务
            int sum = 0;
            for (int i = 0; i < 1000000; i++) {
                sum += i;
            }
        }

        private void lightComputation() {
            // 轻任务几乎不耗时
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

/**
 * ForkJoinPool 核心知识点总结
 *
 * 【什么是 Fork/Join 框架】
 * Java 7 引入的并行计算框架，基于“分治”思想：
 * - Fork：将大任务拆分成小任务
 * - Join：等待小任务完成，合并结果
 *
 * 【核心组件】
 * 1. ForkJoinPool：线程池，管理工作线程
 * 2. ForkJoinTask：任务基类
 *    - RecursiveTask<V>：有返回值的任务
 *    - RecursiveAction：无返回值的任务
 *
 * 【工作流程】
 * 1. 判断任务是否足够小（阈值 THRESHOLD）
 * 2. 小到直接计算，返回结果
 * 3. 大则拆分为子任务
 * 4. 异步执行子任务（fork）
 * 5. 等待子任务结果（join）
 * 6. 合并结果返回
 *
 * 【工作窃取（Work Stealing）】
 * - 每个工作线程维护自己的双端队列（Deque）
 * - 线程从队列头部取任务执行（LIFO）
 * - 自己的任务做完后，从其他线程队列尾部“窃取”任务（FIFO）
 * - 窃取从尾部避免竞争，提高并行效率
 *
 * 【适用场景】
 * ✓ 递归分治算法（归并排序、斐波那契、数组求和）
 * ✓ 树形结构处理（遍历、计算）
 * ✓ 多维数据处理（图像处理、矩阵运算）
 * ✓ 大数据量的并行计算
 *
 * 【不适用的场景】
 * ✗ IO 密集型任务（阻塞会影响整个线程池效率）
 * ✗ 任务间有依赖且不可分割
 * ✗ 数据量太小（拆分开销 > 并行收益）
 *
 * 【与 ThreadPoolExecutor 对比】
 * ForkJoinPool：
 * - 专为分治任务设计
 * - 支持任务拆分和结果合并
 * - 有工作窃取机制
 * - 每个线程有自己的队列
 *
 * ThreadPoolExecutor：
 * - 通用线程池
 * - 任务独立，不能拆分
 * - 全局共享的任务队列
 * - 适合 IO 或独立计算任务
 *
 * 【Java 8 Parallel Stream】
 * Parallel Stream 底层使用 ForkJoinPool.commonPool()
 * 默认并行度 = CPU 核心数
 * 可通过 JVM 参数调整：-Djava.util.concurrent.ForkJoinPool.common.parallelism=N
 *
 * 【最佳实践】
 * 1. 设置合理的阈值（THRESHOLD），避免过度拆分
 * 2. 纯计算密集型任务适合 ForkJoinPool，IO 密集型避免使用
 * 3. 考虑任务的开销，小任务直接用单线程更快
 * 4. 使用 invokeAll() 同时启动多个子任务
 * 5. 避免在任务中阻塞（会阻塞整个工作线程）
 */
