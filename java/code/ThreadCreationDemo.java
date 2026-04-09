package code;

import java.util.concurrent.*;

/**
 * Java 创建多线程的 4 种方式示例
 *
 * 1. 继承 Thread 类
 * 2. 实现 Runnable 接口
 * 3. 实现 Callable 接口（带返回值）
 * 4. 使用线程池（ExecutorService）
 *
 * @author Java面试宝典
 */
public class ThreadCreationDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("Java 创建多线程的 4 种方式演示");
        System.out.println("========================================\n");

        // 方式1：继承 Thread 类
        method1_ExtendThread();

        // 方式2：实现 Runnable 接口
        method2_ImplementRunnable();

        // 方式3：实现 Callable 接口
        method3_ImplementCallable();

        // 方式4：使用线程池
        method4_ThreadPool();

        System.out.println("\n========================================");
        System.out.println("所有线程执行完毕！");
        System.out.println("========================================");
    }

    /**
     * 方式1：继承 Thread 类
     */
    private static void method1_ExtendThread() throws InterruptedException {
        System.out.println("【方式1】继承 Thread 类");
        System.out.println("----------------------------------------");

        // 创建线程1
        MyThread thread1 = new MyThread("Thread-A");

        // 创建线程2（使用匿名内部类）
        Thread thread2 = new Thread("Thread-B") {
            @Override
            public void run() {
                for (int i = 0; i < 3; i++) {
                    System.out.println(getName() + " 运行第 " + (i + 1) + " 次");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        // 启动线程
        thread1.start();
        thread2.start();

        // 等待线程结束
        thread1.join();
        thread2.join();
        System.out.println();
    }

    /**
     * 方式2：实现 Runnable 接口
     */
    private static void method2_ImplementRunnable() throws InterruptedException {
        System.out.println("【方式2】实现 Runnable 接口");
        System.out.println("----------------------------------------");

        // 创建 Runnable 实现类
        MyRunnable runnable1 = new MyRunnable("Runnable-A");

        // 使用 Lambda 表达式创建 Runnable
        Runnable runnable2 = () -> {
            for (int i = 0; i < 3; i++) {
                System.out.println(Thread.currentThread().getName() +
                    " (Lambda) 运行第 " + (i + 1) + " 次");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        // 创建线程并启动
        Thread thread1 = new Thread(runnable1, "Thread-A");
        Thread thread2 = new Thread(runnable2, "Thread-B");

        thread1.start();
        thread2.start();

        // 等待线程结束
        thread1.join();
        thread2.join();
        System.out.println();
    }

    /**
     * 方式3：实现 Callable 接口（带返回值）
     */
    private static void method3_ImplementCallable() throws Exception {
        System.out.println("【方式3】实现 Callable 接口（带返回值）");
        System.out.println("----------------------------------------");

        // 创建 Callable 实现类
        Callable<String> callable1 = new MyCallable("Callable-A");

        // 使用 Lambda 表达式创建 Callable
        Callable<String> callable2 = () -> {
            int sum = 0;
            for (int i = 1; i <= 10; i++) {
                sum += i;
                System.out.println(Thread.currentThread().getName() +
                    " (Lambda) 计算: 1+2+...+" + i + "=" + sum);
                Thread.sleep(50);
            }
            return "Lambda计算完成，1到10的和为:" + sum;
        };

        // 创建 FutureTask
        FutureTask<String> futureTask1 = new FutureTask<>(callable1);
        FutureTask<String> futureTask2 = new FutureTask<>(callable2);

        // 启动线程
        new Thread(futureTask1, "Thread-A").start();
        new Thread(futureTask2, "Thread-B").start();

        // 获取返回值（会阻塞直到线程完成）
        System.out.println("【返回结果】" + futureTask1.get());
        System.out.println("【返回结果】" + futureTask2.get());
        System.out.println();
    }

    /**
     * 方式4：使用线程池（推荐方式）
     */
    private static void method4_ThreadPool() throws InterruptedException {
        System.out.println("【方式4】使用线程池（ExecutorService）");
        System.out.println("----------------------------------------");

        // 创建固定大小的线程池
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // 提交 Runnable 任务
        executor.execute(() -> {
            for (int i = 0; i < 3; i++) {
                System.out.println(Thread.currentThread().getName() +
                    " (Runnable任务) 执行第 " + (i + 1) + " 次");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        // 提交 Callable 任务（带返回值）
        Future<Integer> future = executor.submit(() -> {
            int result = 1;
            for (int i = 1; i <= 5; i++) {
                result *= i;
                System.out.println(Thread.currentThread().getName() +
                    " (Callable任务) 计算: " + i + "! = " + result);
                Thread.sleep(50);
            }
            return result;
        });

        // 获取 Callable 返回值
        try {
            Integer result = future.get();
            System.out.println("【返回结果】5的阶乘 = " + result);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        // 使用 CompletableFuture（Java 8+ 推荐）
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "CompletableFuture 任务完成";
        }, executor);

        try {
            System.out.println("【返回结果】" + completableFuture.get());
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        // 关闭线程池
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println();
    }
}

/**
 * 继承 Thread 类的实现
 */
class MyThread extends Thread {
    public MyThread(String name) {
        super(name);
    }

    @Override
    public void run() {
        for (int i = 0; i < 3; i++) {
            System.out.println(getName() + " 运行第 " + (i + 1) + " 次");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

/**
 * 实现 Runnable 接口的实现
 */
class MyRunnable implements Runnable {
    private String name;

    public MyRunnable(String name) {
        this.name = name;
    }

    @Override
    public void run() {
        for (int i = 0; i < 3; i++) {
            System.out.println(Thread.currentThread().getName() +
                " (" + name + ") 运行第 " + (i + 1) + " 次");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

/**
 * 实现 Callable 接口的实现（带返回值）
 */
class MyCallable implements Callable<String> {
    private String name;

    public MyCallable(String name) {
        this.name = name;
    }

    @Override
    public String call() throws Exception {
        int sum = 0;
        for (int i = 1; i <= 10; i++) {
            sum += i;
            System.out.println(Thread.currentThread().getName() +
                " (" + name + ") 计算: 1+2+...+" + i + "=" + sum);
            Thread.sleep(50);
        }
        return name + " 计算完成，1到10的和为:" + sum;
    }
}
