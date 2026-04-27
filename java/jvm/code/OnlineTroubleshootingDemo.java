import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * 线上问题排查演示代码
 *
 * 模拟以下问题场景：
 * 1. CPU 100% - 死循环、复杂计算、正则回溯
 * 2. 内存泄漏 - 静态集合、ThreadLocal、未关闭资源
 * 3. 线程死锁 - 互相等待
 * 4. Full GC - 频繁创建对象
 *
 * 配合排查工具使用：
 * - top + top -Hp <pid> + jstack
 * - jmap -dump
 * - Arthas: thread/dashboard/watch/trace
 *
 * @author Java面试宝典
 */
public class OnlineTroubleshootingDemo {

    // 内存泄漏场景1：静态集合
    private static final List<byte[]> MEMORY_LEAK_LIST = new ArrayList<>();

    // 内存泄漏场景2：ThreadLocal
    private static final ThreadLocal<String> THREAD_LOCAL = new ThreadLocal<>();

    private volatile boolean running = true;

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("线上问题排查演示程序");
        System.out.println("========================================");
        System.out.println("PID: " + ProcessHandle.current().pid());
        System.out.println(
            "使用 jstack " + ProcessHandle.current().pid() + " 查看线程堆栈"
        );
        System.out.println(
            "使用 Arthas: java -jar arthas-boot.jar " +
                ProcessHandle.current().pid()
        );
        System.out.println();

        OnlineTroubleshootingDemo demo = new OnlineTroubleshootingDemo();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("请选择要演示的问题场景：");
            System.out.println("1. CPU 100% - 死循环");
            System.out.println("2. CPU 100% - 复杂正则回溯");
            System.out.println("3. CPU 100% - 密集计算");
            System.out.println("4. 内存泄漏 - 静态集合");
            System.out.println("5. 内存泄漏 - ThreadLocal");
            System.out.println("6. 线程死锁");
            System.out.println("7. Full GC压力");
            System.out.println("8. 展示正常运行的线程");
            System.out.println("9. CPU高的问题方法（可Arthas监控）");
            System.out.println("0. 退出");
            System.out.print("输入选项: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    demo.simulateDeadLoop();
                    break;
                case "2":
                    demo.simulateRegexBacktracking();
                    break;
                case "3":
                    demo.simulateIntensiveCalculation();
                    break;
                case "4":
                    demo.simulateStaticMemoryLeak();
                    break;
                case "5":
                    demo.simulateThreadLocalLeak();
                    break;
                case "6":
                    demo.simulateDeadlock();
                    break;
                case "7":
                    demo.simulateFullGCPressure();
                    break;
                case "8":
                    demo.simulateNormalOperation();
                    break;
                case "9":
                    demo.simulateProblemMethod();
                    break;
                case "0":
                    System.out.println("退出程序...");
                    System.exit(0);
                default:
                    System.out.println("无效选项");
            }

            System.out.println();
        }
    }

    /**
     * 场景1：死循环导致CPU 100%
     * 排查命令：top -Hp <pid> -> printf "%x\n" <tid> -> jstack <pid> | grep <nid>
     */
    private void simulateDeadLoop() {
        System.out.println("【场景1】启动死循环线程，CPU将飙升...");
        System.out.println("线程名: DeadLoopThread");

        Thread deadLoopThread = new Thread(
            () -> {
                long count = 0;
                while (running) {
                    // 死循环，无终止条件
                    count++;
                    if (count % 1000000000 == 0) {
                        System.out.println("死循环计数: " + count);
                    }
                }
            },
            "DeadLoopThread"
        );

        deadLoopThread.setDaemon(true);
        deadLoopThread.start();

        System.out.println("死循环线程已启动！");
        System.out.println(
            "排查命令：jstack " +
                ProcessHandle.current().pid() +
                " | grep -A 20 DeadLoopThread"
        );
    }

    /**
     * 场景2：正则表达式灾难性回溯导致CPU 100%
     */
    private void simulateRegexBacktracking() {
        System.out.println("【场景2】启动正则回溯线程...");
        System.out.println("线程名: RegexThread");

        Thread regexThread = new Thread(
            () -> {
                // 灾难性回溯的正则：对长字符串匹配困难
                String regex = "(a+)+b";
                String input = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaac";

                try {
                    System.out.println(
                        "尝试匹配长字符串，这将导致灾难性回溯..."
                    );
                    boolean matches = Pattern.matches(regex, input);
                    System.out.println("匹配结果: " + matches);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            },
            "RegexThread"
        );

        regexThread.setDaemon(true);
        regexThread.start();

        System.out.println("正则回溯线程已启动！");
    }

    /**
     * 场景3：密集计算导致CPU高
     */
    private void simulateIntensiveCalculation() {
        System.out.println("【场景3】启动密集计算线程...");
        System.out.println("线程名: CalculationThread");

        Thread calcThread = new Thread(
            () -> {
                Random random = new Random();
                while (running) {
                    // 大量数学计算
                    double result = 0;
                    for (int i = 0; i < 10000000; i++) {
                        result += Math.sin(i) * Math.cos(i) + Math.sqrt(i + 1);
                    }
                    if (random.nextInt(100) == 0) {
                        System.out.println("计算结果: " + result);
                    }
                }
            },
            "CalculationThread"
        );

        calcThread.setDaemon(true);
        calcThread.start();

        System.out.println("密集计算线程已启动！");
    }

    /**
     * 场景4：静态集合内存泄漏
     * 排查：jmap -histo <pid> | head -30
     */
    private void simulateStaticMemoryLeak() {
        System.out.println("【场景4】启动内存泄漏（静态集合）...");
        System.out.println("线程名: MemoryLeakThread");

        Thread leakThread = new Thread(
            () -> {
                int count = 0;
                while (running && count < 1000) {
                    // 不断往静态集合添加大对象，永不释放
                    byte[] data = new byte[1024 * 1024]; // 1MB
                    MEMORY_LEAK_LIST.add(data);
                    count++;

                    System.out.println(
                        "已泄漏内存: " +
                            count +
                            " MB，集合大小: " +
                            MEMORY_LEAK_LIST.size()
                    );

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println(
                    "内存泄漏演示完成（已添加 " + count + " MB到静态集合）"
                );
            },
            "MemoryLeakThread"
        );

        leakThread.setDaemon(true);
        leakThread.start();

        System.out.println("内存泄漏线程已启动！");
        System.out.println(
            "排查命令：jmap -histo " +
                ProcessHandle.current().pid() +
                " | head -20"
        );
    }

    /**
     * 场景5：ThreadLocal内存泄漏
     * 场景：使用ThreadLocal后未remove
     */
    private void simulateThreadLocalLeak() {
        System.out.println("【场景5】启动ThreadLocal内存泄漏...");
        System.out.println("线程名: ThreadLocalLeakPool");

        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 100; i++) {
            final int taskId = i;
            executor.submit(() -> {
                // 设置ThreadLocal但不remove
                THREAD_LOCAL.set("大对象数据" + taskId + new byte[1024 * 1024]);

                // 模拟业务处理
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // 错误：没有调用 THREAD_LOCAL.remove();
                // 在线程池场景下，线程复用会导致内存泄漏

                if (taskId % 10 == 0) {
                    System.out.println(
                        "Task-" + taskId + " 完成，但未remove ThreadLocal！"
                    );
                }
            });
        }

        executor.shutdown();
        System.out.println(
            "ThreadLocal泄漏场景已启动（100个任务提交到线程池）"
        );
        System.out.println("问题：线程池线程复用，ThreadLocal未remove导致泄漏");
    }

    /**
     * 场景6：线程死锁
     * 排查：jstack -l <pid> | grep -A 50 "deadlock"
     */
    private void simulateDeadlock() {
        System.out.println("【场景6】模拟线程死锁...");

        final Object lockA = new Object();
        final Object lockB = new Object();

        Thread thread1 = new Thread(
            () -> {
                synchronized (lockA) {
                    System.out.println("线程1：持有lockA");
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("线程1：尝试获取lockB...");
                    synchronized (lockB) {
                        System.out.println("线程1：获取lockB成功");
                    }
                }
            },
            "DeadlockThread-1"
        );

        Thread thread2 = new Thread(
            () -> {
                synchronized (lockB) {
                    System.out.println("线程2：持有lockB");
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("线程2：尝试获取lockA...");
                    synchronized (lockA) {
                        System.out.println("线程2：获取lockA成功");
                    }
                }
            },
            "DeadlockThread-2"
        );

        thread1.start();
        thread2.start();

        System.out.println("死锁线程已启动！");
        System.out.println(
            "排查命令：jstack -l " +
                ProcessHandle.current().pid() +
                " | grep -A 50 deadlock"
        );
    }

    /**
     * 场景7：Full GC压力 - 频繁创建和丢弃对象
     */
    private void simulateFullGCPressure() {
        System.out.println("【场景7】启动Full GC压力测试...");
        System.out.println("线程名: GCPressureThread");

        Thread gcThread = new Thread(
            () -> {
                List<Object> list = new ArrayList<>();

                while (running) {
                    // 快速创建大量临时对象
                    for (int i = 0; i < 100000; i++) {
                        list.add(new byte[100]); // 小对象快速进入Eden
                    }

                    // 只保留最后一个引用，让前面99%成为垃圾
                    if (!list.isEmpty()) {
                        Object last = list.get(list.size() - 1);
                        list.clear();
                        list.add(last);
                    }

                    // 触发GC
                    System.gc();

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            },
            "GCPressureThread"
        );

        gcThread.setDaemon(true);
        gcThread.start();

        System.out.println("GC压力线程已启动！");
        System.out.println(
            "监控命令：jstat -gc " + ProcessHandle.current().pid() + " 1000"
        );
    }

    /**
     * 场景8：正常运行的多线程
     * 用于演示Arthas的thread和dashboard命令
     */
    private void simulateNormalOperation() {
        System.out.println("【场景8】启动正常的后台业务线程...");

        // 启动多个模拟业务线程
        String[] threadNames = {
            "OrderProcessor",
            "PaymentHandler",
            "InventoryChecker",
            "ReportGenerator",
        };

        for (String name : threadNames) {
            Thread thread = new Thread(
                () -> {
                    Random random = new Random();
                    while (running) {
                        try {
                            // 模拟业务处理
                            simulateBusinessLogic(name);

                            // 随机休眠
                            Thread.sleep(random.nextInt(1000) + 500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                },
                name
            );
            thread.setDaemon(true);
            thread.start();
            System.out.println("启动线程: " + name);
        }

        System.out.println("业务线程已启动，可以使用Arthas监控：");
        System.out.println("arthas> dashboard");
        System.out.println("arthas> thread -n 5");
        System.out.println(
            "arthas> trace com.example.OrderProcessor processOrder"
        );
    }

    /**
     * 场景9：可被Arthas监控的方法
     * 模拟一个慢查询方法
     */
    private void simulateProblemMethod() {
        System.out.println("【场景9】启动问题方法调用...");
        System.out.println("可以使用 Arthas 监控：");
        System.out.println(
            "arthas> trace code.OnlineTroubleshootingDemo slowQueryMethod '#cost>100'"
        );
        System.out.println(
            "arthas> watch code.OnlineTroubleshootingDemo slowQueryMethod '{params,returnObj}'"
        );

        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ProblemMethodThread");
            t.setDaemon(true);
            return t;
        });

        executor.submit(() -> {
            Random random = new Random();
            while (running) {
                try {
                    // 调用可能被监控的方法
                    String result = slowQueryMethod(
                        "user_" + random.nextInt(1000)
                    );
                    if (random.nextInt(10) == 0) {
                        System.out.println("查询结果: " + result);
                    }
                    Thread.sleep(random.nextInt(500));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        System.out.println("问题方法线程已启动！");
    }

    /**
     * 可被Arthas监控的慢方法
     */
    public String slowQueryMethod(String userId) {
        // 模拟数据库慢查询
        Random random = new Random();

        try {
            // 50%概率模拟慢查询
            if (random.nextBoolean()) {
                Thread.sleep(random.nextInt(200) + 50); // 50-250ms
            } else {
                Thread.sleep(20); // 快查询
            }

            // 模拟复杂处理
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                sb.append(userId);
            }

            return "UserData:" + userId + "_length:" + sb.length();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "error";
        }
    }

    /**
     * 模拟业务逻辑
     */
    private void simulateBusinessLogic(String threadName) {
        try {
            Random random = new Random();
            long start = System.currentTimeMillis();

            // 模拟不同业务逻辑
            switch (threadName) {
                case "OrderProcessor":
                    // 订单处理
                    Thread.sleep(random.nextInt(100));
                    processOrder("ORDER_" + random.nextInt(10000));
                    break;
                case "PaymentHandler":
                    // 支付处理
                    Thread.sleep(random.nextInt(200));
                    processPayment(random.nextDouble() * 1000);
                    break;
                case "InventoryChecker":
                    // 库存检查
                    Thread.sleep(random.nextInt(50));
                    checkInventory(random.nextInt(100));
                    break;
                case "ReportGenerator":
                    // 报表生成
                    Thread.sleep(random.nextInt(500));
                    generateReport();
                    break;
            }

            long cost = System.currentTimeMillis() - start;
            if (random.nextInt(20) == 0) {
                System.out.println(threadName + " 处理耗时: " + cost + "ms");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 以下方法用于模拟业务逻辑
    private void processOrder(String orderId) {
        // 订单处理逻辑
    }

    private void processPayment(double amount) {
        // 支付处理逻辑
    }

    private void checkInventory(int productId) {
        // 库存检查逻辑
    }

    private void generateReport() {
        // 报表生成逻辑
    }
}

/**
 * 线上问题排查总结
 *
 * 【CPU 100% 排查流程】
 * 1. top 查看CPU高的进程PID
 * 2. top -Hp <pid> 查看CPU高的线程TID
 * 3. printf "%x\n" <tid> 转为16进制（如162e）
 * 4. jstack <pid> | grep -A 20 <16进制tid> 查看堆栈
 * 5. 定位代码问题（死循环、正则回溯、死锁）
 *
 * 【使用 Arthas 排查 CPU】
 * 1. thread -n 3    查看CPU最高的3个线程
 * 2. thread <tid>   查看线程堆栈
 * 3. trace <class> <method>  监控方法耗时
 *
 * 【内存泄漏排查】
 * 1. jmap -dump:format=b,file=heap.hprof <pid>
 * 2. MAT工具分析：
 *    - Histogram: 查看对象数量
 *    - Dominator Tree: 查看大对象
 *    - Path to GC Roots: 查看引用链
 *
 * 【使用 Arthas 排查内存】
 * 1. dashboard 查看内存使用情况
 * 2. vmtool --action getInstances 获取对象实例
 * 3. sc -d <class> 查看类加载器
 *
 * 【常用命令速查】
 * jstack <pid>           线程堆栈
 * jmap -histo <pid>      对象统计
 * jmap -dump <pid>       堆转储
 * jstat -gc <pid> 1000   GC监控
 * jcmd <pid> VM.flags    查看JVM参数
 */
