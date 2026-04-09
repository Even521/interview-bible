package code;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ThreadLocal 内存泄漏导致 OOM 演示
 *
 * 这个程序会真实地发生内存溢出（OutOfMemoryError）
 *
 * 运行命令：
 * javac -d java/target java/code/ThreadLocalOOM.java
 * java -Xms32m -Xmx32m -cp java/target code.ThreadLocalOOM
 *
 * 预期结果：最终会抛出 java.lang.OutOfMemoryError: Java heap space
 */
public class ThreadLocalOOM {

    // 每个对象占用 5MB 内存，更容易触发 OOM
    static class LargeObject {

        private final byte[] data = new byte[5 * 1024 * 1024]; // 5MB
        private final String name;

        public LargeObject(String name) {
            this.name = name;
        }
    }

    // 用于保持 ThreadLocal 引用，防止被 GC
    private static final List<ThreadLocal<LargeObject>> threadLocalList =
        new ArrayList<>();

    // 固定 2 个线程的线程池
    private static final ExecutorService executor =
        Executors.newFixedThreadPool(2);

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("ThreadLocal 内存泄漏导致 OOM 演示");
        System.out.println("========================================");
        System.out.println();
        System.out.println(" JVM 堆内存限制：-Xms64m -Xmx64m (64MB)");
        System.out.println(" 每个对象大小：5MB");
        System.out.println(" 线程池大小：2个线程（复用）");
        System.out.println("【关键】每个任务创建新的 ThreadLocal，且不 remove");
        System.out.println();

        Runtime runtime = Runtime.getRuntime();

        try {
            // 提交 20 个任务，每个 5MB，总共 100MB，远超 64MB
            for (int i = 0; i < 20; i++) {
                final int taskId = i;

                // 为每个任务创建新的 ThreadLocal
                // 这样不会覆盖之前的值，而是会在 ThreadLocalMap 中新增 Entry
                ThreadLocal<LargeObject> taskThreadLocal = new ThreadLocal<>();
                threadLocalList.add(taskThreadLocal);

                executor.submit(() -> {
                    try {
                        // 创建 5MB 的大对象
                        LargeObject obj = new LargeObject("Task-" + taskId);
                        taskThreadLocal.set(obj);

                        // 打印信息
                        long usedMemory =
                            (runtime.totalMemory() - runtime.freeMemory()) /
                            1024 /
                            1024;
                        System.out.println(
                            Thread.currentThread().getName() +
                                " 执行 Task-" +
                                taskId +
                                " | 已用内存: " +
                                usedMemory +
                                "MB" +
                                " | 对象: " +
                                obj.name
                        );

                        // 关键：不让线程结束，保持对对象的引用
                        // 模拟长时间运行的任务
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (OutOfMemoryError e) {
                        System.out.println(
                            Thread.currentThread().getName() + " 遇到 OOM!"
                        );
                        throw e;
                    }
                    // ❌ 故意不调用 remove()，造成内存泄漏！
                    // 每个 ThreadLocal 实例在 ThreadLocalMap 中创建一个独立的 Entry
                    // 由于线程池线程复用，这些 Entry 永远不会被清理
                });

                // 每个任务提交后都打印内存
                Thread.sleep(100); // 让小批量任务快速提交
                long usedMemory =
                    (runtime.totalMemory() - runtime.freeMemory()) /
                    1024 /
                    1024;
                long maxMemory = runtime.maxMemory() / 1024 / 1024;
                System.out.println(
                    ">>> [监控] 已提交 " +
                        (i + 1) +
                        " 个任务 | 已用内存: " +
                        usedMemory +
                        "MB / " +
                        maxMemory +
                        "MB"
                );

                // 当内存使用超过 80% 时提示
                if (usedMemory > maxMemory * 0.8) {
                    System.out.println(
                        ">>> [警告] 内存使用超过 80%，即将 OOM!"
                    );
                }
            }

            System.out.println();
            System.out.println(">>> 等待所有任务执行完成...");
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (OutOfMemoryError e) {
            System.out.println();
            System.out.println("========================================");
            System.out.println("内存溢出(OOM)发生了！");
            System.out.println("========================================");
            System.out.println("错误类型: " + e.getClass().getName());
            System.out.println("错误信息: " + e.getMessage());
            System.out.println();
            System.out.println("原因分析:");
            System.out.println(" 1. 每个任务创建独立的 ThreadLocal 实例");
            System.out.println(
                " 2. ThreadLocal.set() 在 ThreadLocalMap 中创建 Entry"
            );
            System.out.println(
                " 3. Entry 的 value 是强引用，指向 5MB 的 LargeObject"
            );
            System.out.println(" 4. 不调用 remove()，这些 Entry 永远存在");
            System.out.println(" 5. 线程池线程复用，ThreadLocalMap 不断累积");
            System.out.println(" 6. 最终耗尽堆内存，抛出 OOM");
            System.out.println();
            System.out.println("解决方案:");
            System.out.println(" - 使用 try-finally 确保调用 remove()");
            System.out.println(" - 复用同一个 ThreadLocal 实例（静态 final）");
            System.out.println(
                " - 或使用 ThreadLocal.withInitial() + 及时清理"
            );
            System.out.println("========================================");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println();
        System.out.println("演示结束！");
    }
}
///e/app/jdk/bin/java -Xms64m -Xmx64m -cp java/target code.ThreadLocalOOM
