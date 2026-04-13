package code;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;

/**
 * CopyOnWriteArrayList 适用场景演示
 *
 * 核心知识点：
 * 1. CopyOnWriteArrayList 是什么（写时复制机制）
 * 2. 适用场景（读多写少）
 * 3. 实现原理（读写分离，读不加锁，写加锁并复制新数组）
 * 4. 内存占用和性能特点
 * 5. 迭代器弱一致性
 * 6. 与 ArrayList、Collections.synchronizedList、Vector 的对比
 *
 * @author Java面试宝典
 */
public class CopyOnWriteArrayListDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("CopyOnWriteArrayList 适用场景演示");
        System.out.println("========================================\n");

        // 1. 基本使用
        demonstrateBasicUsage();

        // 2. 写时复制原理
        demonstrateCopyOnWriteMechanism();

        // 3. 迭代器安全性（弱一致性）
        demonstrateIteratorSafety();

        // 4. 性能对比测试
        demonstratePerformanceComparison();

        // 5. 适用场景分析
        demonstrateUseCases();

        // 6. 不适合的场景
        demonstrateUnsuitableCases();

        // 7. 与其他线程安全List对比
        demonstrateComparison();

        System.out.println("\n========================================");
        System.out.println("演示结束！");
        System.out.println("========================================");
    }

    /**
     * 演示基本使用
     */
    private static void demonstrateBasicUsage() {
        System.out.println("【演示1】CopyOnWriteArrayList 基本使用");
        System.out.println("----------------------------------------");

        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();

        // 添加元素（写操作，会加锁并复制数组）
        list.add("Alice");
        list.add("Bob");
        list.add("Charlie");

        System.out.println("初始列表: " + list);

        // 获取元素（读操作，无锁，直接读快照）
        String first = list.get(0);
        System.out.println("get(0): " + first);

        // 遍历（读操作，遍历的是获取迭代器时的快照）
        System.out.println("遍历:");
        for (String name : list) {
            System.out.println("  " + name);
        }

        // 删除（写操作）
        list.remove("Bob");
        System.out.println("删除 Bob 后: " + list);

        System.out.println("\n【特点总结】");
        System.out.println(" - 读操作：无锁，直接访问底层数组");
        System.out.println(" - 写操作：加锁，复制新数组，替换引用");
        System.out.println(" - 迭代器：弱一致性，遍历的是快照");
        System.out.println();
    }

    /**
     * 演示写时复制原理
     */
    private static void demonstrateCopyOnWriteMechanism() {
        System.out.println("【演示2】写时复制（Copy-On-Write）原理");
        System.out.println("----------------------------------------");

        System.out.println("【写操作流程】");
        System.out.println("1. 加锁（ReentrantLock）");
        System.out.println("2. 复制原数组到新数组（Arrays.copyOf）");
        System.out.println("3. 在新数组上执行写操作（add/remove/set）");
        System.out.println("4. 用新数组替换原数组引用");
        System.out.println("5. 解锁");

        System.out.println("\n【源码简化】");
        System.out.println("public boolean add(E e) {");
        System.out.println("    final ReentrantLock lock = this.lock;");
        System.out.println("    lock.lock();");
        System.out.println("    try {");
        System.out.println("        Object[] elements = getArray();");
        System.out.println("        int len = elements.length;");
        System.out.println("        Object[] newElements = Arrays.copyOf(elements, len + 1);");
        System.out.println("        newElements[len] = e;");
        System.out.println("        setArray(newElements); // 原子替换引用");
        System.out.println("        return true;");
        System.out.println("    } finally {");
        System.out.println("        lock.unlock();");
        System.out.println("    }");
        System.out.println("}");

        System.out.println("\n【读操作流程】");
        System.out.println("public E get(int index) {");
        System.out.println("    return get(getArray(), index); // 无锁！");
        System.out.println("}");

        System.out.println("\n【内存图示】");
        System.out.println("写操作前:");
        System.out.println("  Thread A (读) -> [A, B, C] <- Thread B (读)");
        System.out.println("  Thread C (写) 加锁...");
        System.out.println();
        System.out.println("写操作中:");
        System.out.println("  Thread A (读) -> [A, B, C] (旧快照)");
        System.out.println("  Thread C (写) 复制新数组 [A, B, C, D]");
        System.out.println();
        System.out.println("写操作后（原子替换）:");
        System.out.println("  Thread A (读) -> [A, B, C] (旧数组，仍可读)");
        System.out.println("  Thread B (读) -> [A, B, C, D] (新数组)");
        System.out.println("  Old array [A, B, C] 等待 GC");
        System.out.println();
    }

    /**
     * 演示迭代器安全性（弱一致性）
     */
    private static void demonstrateIteratorSafety() throws InterruptedException {
        System.out.println("【演示3】迭代器弱一致性（快照特性）");
        System.out.println("----------------------------------------");

        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
        list.add("A");
        list.add("B");
        list.add("C");

        System.out.println("初始列表: " + list);

        // 获取迭代器（创建当前数组的快照）
        Iterator<String> iterator = list.iterator();

        System.out.println("\n在遍历过程中修改列表...");

        // 修改列表（添加元素D）
        list.add("D");
        System.out.println("添加 D 后列表: " + list);

        // 遍历迭代器（看到的是旧快照，不包含D）
        System.out.println("迭代器遍历结果（快照，不包含D）:");
        while (iterator.hasNext()) {
            System.out.println("  " + iterator.next());
        }

        // 新的迭代器能看到D
        System.out.println("\n新迭代器能看到D:");
        Iterator<String> newIterator = list.iterator();
        while (newIterator.hasNext()) {
            System.out.println("  " + newIterator.next());
        }

        System.out.println("\n【For-each 修改异常对比】");
        System.out.println("【ArrayList 的情况】");
        List<String> arrayList = new ArrayList<>();
        arrayList.add("A");
        arrayList.add("B");

        try {
            for (String s : arrayList) {
                if (s.equals("B")) {
                    arrayList.remove(s); // 修改原集合
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("ArrayList: 抛出 ConcurrentModificationException！");
        }

        System.out.println("\n【CopyOnWriteArrayList 的情况】");
        CopyOnWriteArrayList<String> cowList = new CopyOnWriteArrayList<>();
        cowList.add("A");
        cowList.add("B");

        for (String s : cowList) {
            if (s.equals("B")) {
                cowList.remove(s); // 安全！遍历的是快照，修改不影响迭代器
            }
        }
        System.out.println("CopyOnWriteArrayList: 可以安全遍历并修改，最终列表: " + cowList);

        System.out.println("\n【弱一致性特点】");
        System.out.println("✓ 遍历过程中修改不会抛出 ConcurrentModificationException");
        System.out.println("✓ 迭代器看到的是获取时的快照，不会看到后续修改");
        System.out.println("✗ 可能读到旧数据（非实时一致性）");
        System.out.println();
    }

    /**
     * 性能对比测试
     */
    private static void demonstratePerformanceComparison() throws InterruptedException {
        System.out.println("【演示4】性能对比测试（读多写少 vs 读写均衡）");
        System.out.println("----------------------------------------");

        System.out.println("【场景1：读多写少（90%读，10%写）】");
        int operations = 100000;
        int readRatio = 9; // 9:1 = 90%读

        // 测试 CopyOnWriteArrayList
        CopyOnWriteArrayList<Integer> cowList = new CopyOnWriteArrayList<>();
        long cowTime = testReadHeavyScenario(cowList, operations, readRatio);
        System.out.println("CopyOnWriteArrayList: " + cowTime + " ms");

        // 测试 Collections.synchronizedList
        List<Integer> syncList = Collections.synchronizedList(new ArrayList<>());
        long syncTime = testReadHeavyScenario(syncList, operations, readRatio);
        System.out.println("Collections.synchronizedList: " + syncTime + " ms");

        // 测试 Vector
        List<Integer> vector = new Vector<>();
        long vectorTime = testReadHeavyScenario(vector, operations, readRatio);
        System.out.println("Vector: " + vectorTime + " ms");

        System.out.println("\n【场景2：读写均衡（50%读，50%写）】");
        System.out.println("(CopyOnWriteArrayList 在此场景性能极差，数据量减小)");

        operations = 10000; // 减少数据量
        readRatio = 1; // 1:1 = 50%读

        // CopyOnWriteArrayList
        CopyOnWriteArrayList<Integer> cowList2 = new CopyOnWriteArrayList<>();
        startTime = System.currentTimeMillis();
        for (int i = 0; i < operations; i++) {
            if (i % 2 == 0) {
                cowList2.add(i);
            } else {
                cowList2.get(i / 2);
            }
        }
        long cowTime2 = System.currentTimeMillis() - startTime;
        System.out.println("CopyOnWriteArrayList: " + cowTime2 + " ms (写多性能差)");

        // synchronizedList
        List<Integer> syncList2 = Collections.synchronizedList(new ArrayList<>());
        startTime = System.currentTimeMillis();
        for (int i = 0; i < operations; i++) {
            if (i % 2 == 0) {
                syncList2.add(i);
            } else {
                syncList2.get(i / 2);
            }
        }
        long syncTime2 = System.currentTimeMillis() - startTime;
        System.out.println("Collections.synchronizedList: " + syncTime2 + " ms");

        System.out.println("\n【性能结论】");
        System.out.println("读多写少：CopyOnWriteArrayList >> synchronizedList > Vector");
        System.out.println("读写均衡：CopyOnWriteArrayList << synchronizedList, Vector");
        System.out.println("原因：CopyOnWriteArrayList 每次写都要复制整个数组");
        System.out.println();
    }

    /**
     * 测试读多写少场景
     */
    private static long startTime;

    private static long testReadHeavyScenario(List<Integer> list, int operations, int readRatio) {
        // 初始化数据
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }

        CountDownLatch latch = new CountDownLatch(10);
        startTime = System.currentTimeMillis();

        // 10个线程并发操作
        for (int t = 0; t < 10; t++) {
            final int threadId = t;
            new Thread(() -> {
                Random random = new Random();
                for (int i = 0; i < operations / 10; i++) {
                    if (random.nextInt(readRatio + 1) < readRatio) {
                        // 读操作
                        list.get(random.nextInt(list.size()));
                    } else {
                        // 写操作
                        list.add(random.nextInt(1000));
                    }
                }
                latch.countDown();
            }).start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return System.currentTimeMillis() - startTime;
    }

    /**
     * 演示适用场景
     */
    private static void demonstrateUseCases() {
        System.out.println("【演示5】CopyOnWriteArrayList 适用场景");
        System.out.println("----------------------------------------");

        System.out.println("【场景1：事件监听器列表】");
        System.out.println("特点：监听器注册一次（写少），但事件触发时遍历所有监听器（读多）");
        demonstrateEventListener();

        System.out.println("\n【场景2：配置项列表（读多写少）】");
        System.out.println("特点：配置加载后基本不变，但频繁读取");
        demonstrateConfiguration();

        System.out.println("\n【场景3：路由表/白名单】");
        System.out.println("特点：路由规则很少修改，但每个请求都要检查（高频读）");
        demonstrateRouteTable();

        System.out.println("\n【场景4：缓存键列表】");
        demonstrateCacheKeys();

        System.out.println("\n【适用场景总结】");
        System.out.println("✓ 读操作远多于写操作（读:写 >= 9:1）");
        System.out.println("✓ 数据量不大（避免复制开销过大）");
        System.out.println("✓ 读操作要求弱一致性（可接受读到旧数据）");
        System.out.println("✓ 需要线程安全，但不想自己加锁");
        System.out.println("✓ 遍历过程中需要修改集合");
        System.out.println();
    }

    /**
     * 事件监听器示例
     */
    private static void demonstrateEventListener() {
        System.out.println("\n-- 事件监听器示例 --");

        class EventManager {
            private CopyOnWriteArrayList<EventListener> listeners = new CopyOnWriteArrayList<>();

            public void addListener(EventListener listener) {
                listeners.add(listener);
            }

            public void fireEvent(String event) {
                // 遍历过程中可以安全添加/删除监听器
                for (EventListener listener : listeners) {
                    listener.onEvent(event);
                }
            }
        }

        EventManager manager = new EventManager();

        // 添加监听器
        manager.addListener(e -> System.out.println("Listener 1: " + e));
        manager.addListener(e -> System.out.println("Listener 2: " + e));

        // 触发事件（读操作频繁）
        System.out.println("触发事件:");
        manager.fireEvent("Data Update");
    }

    interface EventListener {
        void onEvent(String event);
    }

    /**
     * 配置项示例
     */
    private static void demonstrateConfiguration() {
        System.out.println("\n-- 配置项示例 --");

        class ConfigManager {
            private CopyOnWriteArrayList<String> whitelist = new CopyOnWriteArrayList<>();

            public void addIp(String ip) {
                whitelist.add(ip);
            }

            public boolean isAllowed(String ip) {
                // 高频读操作
                return whitelist.contains(ip);
            }
        }

        ConfigManager config = new ConfigManager();
        config.addIp("192.168.1.1");
        config.addIp("192.168.1.2");

        System.out.println("检查IP是否允许:");
        System.out.println("  192.168.1.1 " + (config.isAllowed("192.168.1.1") ? "允许" : "拒绝"));
        System.out.println("  10.0.0.1 " + (config.isAllowed("10.0.0.1") ? "允许" : "拒绝"));
    }

    /**
     * 路由表示例
     */
    private static void demonstrateRouteTable() {
        System.out.println("\n-- 路由表示例 --");

        class SimpleRouter {
            private CopyOnWriteArrayList<Route> routes = new CopyOnWriteArrayList<>();

            public void addRoute(String path, String service) {
                routes.add(new Route(path, service));
            }

            public String route(String requestPath) {
                for (Route route : routes) {
                    if (requestPath.startsWith(route.path)) {
                        return route.service;
                    }
                }
                return "DEFAULT";
            }
        }

        SimpleRouter router = new SimpleRouter();
        router.addRoute("/api/users", "UserService");
        router.addRoute("/api/orders", "OrderService");

        System.out.println("路由请求:");
        System.out.println("  /api/users/list -> " + router.route("/api/users/list"));
        System.out.println("  /api/orders/123 -> " + router.route("/api/orders/123"));
    }

    static class Route {
        String path;
        String service;
        Route(String path, String service) {
            this.path = path;
            this.service = service;
        }
    }

    /**
     * 缓存键列表示例
     */
    private static void demonstrateCacheKeys() {
        System.out.println("\n-- 缓存键列表示例 --");

        class CacheKeyManager {
            private CopyOnWriteArrayList<String> hotKeys = new CopyOnWriteArrayList<>();

            public void updateHotKeys(List<String> newKeys) {
                // 批量替换（本质是多次写）
                hotKeys.clear();
                hotKeys.addAll(newKeys);
            }

            public boolean isHotKey(String key) {
                // 高频查询
                return hotKeys.contains(key);
            }
        }

        CacheKeyManager manager = new CacheKeyManager();
        List<String> keys = Arrays.asList("user:1001", "user:1002", "order:500");
        manager.updateHotKeys(keys);

        System.out.println("热键检查:");
        System.out.println("  user:1001 " + (manager.isHotKey("user:1001") ? "是热键" : "非热键"));
        System.out.println("  product:999 " + (manager.isHotKey("product:999") ? "是热键" : "非热键"));
    }

    /**
     * 演示不适合的场景
     */
    private static void demonstrateUnsuitableCases() {
        System.out.println("【演示6】不适合使用 CopyOnWriteArrayList 的场景");
        System.out.println("----------------------------------------");

        System.out.println("【场景1：写操作频繁】");
        System.out.println("❌ 每次写都要复制整个数组，性能极差");
        System.out.println("   替代方案：Collections.synchronizedList 或 ConcurrentLinkedQueue");

        System.out.println("\n【场景2：数据量大】");
        System.out.println("❌ 复制大数组开销大，内存占用高");
        System.out.println("   示例：100万元素的列表，每次添加都要复制100万+1个引用");
        System.out.println("   替代方案：分段锁或其他并发集合");

        System.out.println("\n【场景3：需要实时一致性】");
        System.out.println("❌ 读操作可能看到旧数据（弱一致性）");
        System.out.println("   示例：金融交易系统、库存扣减");
        System.out.println("   替代方案：需要强同步的锁机制");

        System.out.println("\n【场景4：内存敏感】");
        System.out.println("❌ 写操作期间内存中存在两个数组");
        System.out.println("   旧数组用于未完成的读操作，新数组用于新读操作");
        System.out.println("   替代方案：普通 ArrayList + 手动同步（单写多读）");

        System.out.println("\n【不适合场景总结】");
        System.out.println("✗ 写操作频繁（写:读 >= 1:1）");
        System.out.println("✗ 数据量大（元素数 > 1000）");
        System.out.println("✗ 要求强一致性（实时读到最新数据）");
        System.out.println("✗ 内存紧张（无法承受双倍数组开销）");
        System.out.println();
    }

    /**
     * 与其他线程安全List对比
     */
    private static void demonstrateComparison() {
        System.out.println("【演示7】线程安全 List 对比");
        System.out.println("----------------------------------------");

        System.out.println("【实现对比】");

        System.out.println("\n1. CopyOnWriteArrayList");
        System.out.println("   实现原理：写时复制，读写分离");
        System.out.println("   读操作：无锁，直接读快照");
        System.out.println("   写操作：加锁 + 复制新数组");
        System.out.println("   迭代器：弱一致性，不会抛异常");
        System.out.println("   适用场景：读多写少，数据量小");

        System.out.println("\n2. Collections.synchronizedList");
        System.out.println("   实现原理：装饰器模式，方法上加 synchronized");
        System.out.println("   读操作：synchronized（对象锁）");
        System.out.println("   写操作：synchronized（对象锁）");
        System.out.println("   迭代器：需要手动同步，仍可能抛异常");
        System.out.println("   适用场景：读写均衡，需要强一致性");

        System.out.println("\n3. Vector");
        System.out.println("   实现原理：所有方法 synchronized");
        System.out.println("   已过时，不推荐使用");

        System.out.println("\n4. ConcurrentLinkedQueue（作为List使用）");
        System.out.println("   实现原理：无锁（CAS）");
        System.out.println("   读操作：CAS（无锁）");
        System.out.println("   写操作：CAS（无锁）");
        System.out.println("   特点：高并发性能更好，但非List接口");

        System.out.println("\n【对比表格】");
        System.out.println("┌─────────────────────┬──────────┬────────────┬─────────┬──────────┐");
        System.out.println("│ 特性                │ COW-List │ Sync-List  │ Vector  │ CL-Queue │");
        System.out.println("├─────────────────────┼──────────┼────────────┼─────────┼──────────┤");
        System.out.println("│ 读锁                │ 无       │ synchronized │ synchronized │ CAS      │");
        System.out.println("│ 写锁                │ 有       │ synchronized │ synchronized │ CAS      │");
        System.out.println("│ 读多写少性能        │ 极好     │ 一般       │ 一般    │ 好       │");
        System.out.println("│ 读写均衡性能        │ 差       │ 一般       │ 一般    │ 好       │");
        System.out.println("│ 写多性能            │ 极差     │ 一般       │ 差      │ 好       │");
        System.out.println("│ 内存占用            │ 高       │ 低         │ 低      │ 低       │");
        System.out.println("│ 迭代器一致性        │ 弱       │ 强         │ 强      │ 弱       │");
        System.out.println("│ 可迭代中修改        │ ✅       │ ❌需手动   │ ❌需手动 │ ✅       │");
        System.out.println("└─────────────────────┴──────────┴────────────┴─────────┴──────────┘");

        System.out.println("\n【选择建议】");
        System.out.println("• 读多写少 + 数据量小：CopyOnWriteArrayList");
        System.out.println("• 读写均衡 + 需要强一致：Collections.synchronizedList");
        System.out.println("• 高并发 + 队列场景：ConcurrentLinkedQueue / BlockingQueue");
        System.out.println("• 避免使用：Vector（已过时）");
        System.out.println();
    }
}

/**
 * CopyOnWriteArrayList 核心知识点总结
 *
 * 【是什么】
 * CopyOnWriteArrayList 是 java.util.concurrent 包提供的线程安全 List 实现。
 * 它采用"写时复制"（Copy-On-Write）策略实现读写分离，保证线程安全的同时
 * 提供高并发的读性能。
 *
 * 【核心特点】
 * 1. 读操作不加锁：直接读取底层数组，性能极高
 * 2. 写操作加锁并复制：加锁后复制新数组，在新数组上修改，然后原子替换
 * 3. 弱一致性：读操作可能读到旧数据（快照）
 * 4. 迭代器安全：遍历的是获取迭代器时的快照，可边遍历边修改
 *
 * 【源码关键】
 * public class CopyOnWriteArrayList<E> implements List<E> {
 *     // volatile 保证数组引用修改的可见性
 *     private transient volatile Object[] array;
 *
 *     // 读操作：无锁
 *     public E get(int index) {
 *         return get(getArray(), index);
 *     }
 *
 *     // 写操作：加锁 + 复制
 *     public boolean add(E e) {
 *         final ReentrantLock lock = this.lock;
 *         lock.lock();
 *         try {
 *             Object[] elements = getArray();
 *             int len = elements.length;
 *             Object[] newElements = Arrays.copyOf(elements, len + 1);
 *             newElements[len] = e;
 *             setArray(newElements); // 原子替换 array 引用
 *             return true;
 *         } finally {
 *             lock.unlock();
 *         }
 *     }
 * }
 *
 * 【适用场景】
 * ✓ 读操作远多于写操作（读:写 >= 9:1）
 * ✓ 数据量不大（避免复制开销过大）
 * ✓ 需要线程安全又不想自己加锁
 * ✓ 遍历过程中需要修改集合
 * ✓ 可以接受弱一致性（非实时读到最新数据）
 *
 * 【具体应用】
 * 1. 事件监听器列表：监听器注册少（写少），但事件触发频繁遍历（读多）
 * 2. 配置项列表：配置加载后基本不变，频繁读取
 * 3. 路由表/白名单：规则很少修改，但每次请求都要查询
 * 4. 热键/缓存键列表：批量更新，频繁查询
 * 5. 观察者模式中的观察者列表
 *
 * 【不适合场景】
 * ✗ 写操作频繁（每次写都要复制整个数组）
 * ✗ 数据量大（复制开销大，内存占用高）
 * ✗ 要求强一致性（实时读到最新数据，如金融交易）
 * ✗ 内存紧张（写操作期间存在两个数组）
 *
 * 【性能对比】
 *                       读多写少    读写均衡    写多
 * CopyOnWriteArrayList   极好       差        极差
 * Collections.synchronizedList  一般  一般      一般
 * Vector                     一般     一般      差
 * ConcurrentLinkedQueue     好      好        好
 *
 * 【与 ArrayList 对比】
 * CopyOnWriteArrayList 在以下场景优于 ArrayList：
 * 1. 多线程环境下，ArrayList 不是线程安全，需要外部同步
 * 2. CopyOnWriteArrayList 的读并发性能远高于 ArrayList + synchronized
 * 3. 但单线程或写多场景，CopyOnWriteArrayList 性能差于 ArrayList
 *
 * 【最佳实践】
 * 1. 评估读写的比例，确保读:写 >= 9:1 才使用
 * 2. 评估数据量大小，数据量大时避免使用
 * 3. 写操作可以批量进行，减少复制次数
 * 4. 使用迭代器遍历时，注意读取的是快照（可能旧数据）
 * 5. 考虑内存占用，写操作期间会存在两个数组
 */
