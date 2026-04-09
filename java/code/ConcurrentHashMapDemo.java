package code;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ConcurrentHashMap 实现原理演示
 *
 * 重点展示：
 * 1. JDK7 分段锁（Segment）实现原理
 * 2. JDK8 CAS + synchronized 实现原理
 * 3. 为什么 JDK8 放弃分段锁
 * 4. 并发性能测试
 *
 * @author Java面试宝典
 */
public class ConcurrentHashMapDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("ConcurrentHashMap 实现原理演示");
        System.out.println("========================================\n");

        // 1. JDK7 vs JDK8 实现对比
        demonstrateJDKComparison();

        // 2. 并发性能对比
        demonstratePerformance();

        // 3. size() 方法原理
        demonstrateSizeMethod();

        // 4. 常用原子操作
        demonstrateAtomicOperations();

        // 5. 并发场景演示
        demonstrateConcurrentScenario();

        System.out.println("\n========================================");
        System.out.println("演示结束！");
        System.out.println("========================================");
    }

    /**
     * 对比 JDK7 和 JDK8 的实现原理
     */
    private static void demonstrateJDKComparison() {
        System.out.println("【JDK7 分段锁 vs JDK8 CAS + synchronized】");
        System.out.println("========================================");

        System.out.println("\n【JDK7 分段锁实现】");
        System.out.println("----------------------------------------");
        System.out.println("数据结构：");
        System.out.println("  ConcurrentHashMap");
        System.out.println("  └── Segment[16]（默认16个分段）");
        System.out.println("      ├── Segment 0: HashEntry[]");
        System.out.println("      ├── Segment 1: HashEntry[]");
        System.out.println("      └── Segment 15: HashEntry[]");
        System.out.println();
        System.out.println("锁机制：");
        System.out.println("  - 每个 Segment 继承 ReentrantLock");
        System.out.println("  - 默认 16 个 Segment，最多支持 16 个线程并发写");
        System.out.println("  - 读操作不需要锁（volatile 保证可见性）");
        System.out.println();
        System.out.println("put 操作流程：");
        System.out.println("  1. 计算 hash，确定 Segment 索引");
        System.out.println(
            "     segment = (hash >>> segmentShift) & segmentMask"
        );
        System.out.println("  2. 获取 Segment 的锁");
        System.out.println("  3. 在 Segment 内定位 HashEntry 位置");
        System.out.println("  4. 遍历链表，key 存在则覆盖，否则插入新节点");
        System.out.println("  5. 释放锁");
        System.out.println();
        System.out.println("JDK7 的局限：");
        System.out.println("  - Segment 是数组，size 不可变");
        System.out.println("  - 最大并发度受限于 Segment 数量");
        System.out.println("  - size() 计算需要遍历所有 Segment，可能需要加锁");
        System.out.println("  - 每个 Segment 是一个独立的小 HashMap，维护复杂");

        System.out.println("\n【JDK8 CAS + synchronized 实现】");
        System.out.println("----------------------------------------");
        System.out.println("数据结构：");
        System.out.println("  ConcurrentHashMap");
        System.out.println("  └── Node[] table");
        System.out.println("      ├── index 0 → null/Node/TreeBin");
        System.out.println("      ├── index 1 → Node → Node → TreeBin");
        System.out.println("      └── index n → Node → Node");
        System.out.println();
        System.out.println("锁机制：");
        System.out.println("  - 读操作：无锁，使用 volatile 保证可见性");
        System.out.println("  - 写操作（空桶）：CAS 直接插入");
        System.out.println("  - 写操作（非空桶）：synchronized 锁链表头节点");
        System.out.println("  - 锁粒度：桶级别（table.length 个锁）");
        System.out.println();
        System.out.println("put 操作流程：");
        System.out.println("  1. 计算 hash，定位 table 索引");
        System.out.println("     index = (n - 1) & hash");
        System.out.println("  2. 如果 table[index] == null：");
        System.out.println("     - CAS 尝试插入新节点（无锁）");
        System.out.println("     - 失败则进入自旋或加锁逻辑");
        System.out.println("  3. 如果 table[index] != null：");
        System.out.println("     - synchronized(table[index]) 锁住头节点");
        System.out.println("     - 遍历链表/红黑树插入或更新");
        System.out.println("  4. 检查是否需要树化（链表长度 >= 8）");
        System.out.println("  5. 检查是否需要扩容（size > threshold）");
        System.out.println("  6. 释放锁");
        System.out.println();
        System.out.println("JDK8 的优势：");
        System.out.println("  - 锁粒度更细：桶级锁 vs Segment 级锁");
        System.out.println("  - 并发度更高：理论上无上限（桶数量可变）");
        System.out.println("  - 读操作完全无锁：性能极高");
        System.out.println("  - size() 优化：CounterCell 分散热点，无需全局锁");
        System.out.println("  - 扩容优化：多线程协助迁移数据");
        System.out.println("  - 红黑树优化：长链查询从 O(n) 降到 O(log n)");

        System.out.println("\n【为什么 JDK8 放弃分段锁？】");
        System.out.println("----------------------------------------");
        System.out.println("1. synchronized 性能大幅提升（JDK6+）");
        System.out.println("   - 引入偏向锁、轻量级锁、重量级锁升级机制");
        System.out.println(
            "   - 大部分场景下 synchronized 性能不逊于 ReentrantLock"
        );
        System.out.println();
        System.out.println("2. 实现更简单，维护更容易");
        System.out.println("   - Segment 机制复杂，维护成本高");
        System.out.println("   - CAS + synchronized 逻辑更清晰");
        System.out.println();
        System.out.println("3. 更高的并发度");
        System.out.println("   - Segment 数量固定（默认16）");
        System.out.println("   - JDK8 锁粒度是桶级别，随扩容而增加");
        System.out.println();
        System.out.println("4. 更好的性能优化空间");
        System.out.println("   - 可以完全无锁的读操作");
        System.out.println("   - CounterCell 分散 size 计算热点");
        System.out.println("   - 多线程协助扩容");
    }

    /**
     * 并发性能对比
     */
    private static void demonstratePerformance() throws Exception {
        System.out.println("\n【并发性能对比测试】");
        System.out.println("========================================");

        int threadCount = 16;
        int operationsPerThread = 10000;

        // 测试 Hashtable
        System.out.println("\n测试 Hashtable（全表锁）：");
        long hashtableTime = testMapPerformance(
            new Hashtable<>(),
            threadCount,
            operationsPerThread
        );
        System.out.println("  总耗时: " + hashtableTime + " ms");

        // 测试 ConcurrentHashMap
        System.out.println("\n测试 ConcurrentHashMap（桶级锁）：");
        long chmTime = testMapPerformance(
            new ConcurrentHashMap<>(),
            threadCount,
            operationsPerThread
        );
        System.out.println("  总耗时: " + chmTime + " ms");

        // 计算性能提升
        double speedup = (double) hashtableTime / chmTime;
        System.out.println("\n【性能对比结果】");
        System.out.printf(
            "ConcurrentHashMap 比 Hashtable 快 %.2f 倍%n",
            speedup
        );
        System.out.println();
        System.out.println("原因分析：");
        System.out.println(
            "  Hashtable：所有读写操作竞争同一把锁（synchronized 方法）"
        );
        System.out.println("  ConcurrentHashMap：");
        System.out.println("    - 读操作：无锁，直接读取 volatile");
        System.out.println("    - 写操作：只锁单个桶，不同桶可并发");
        System.out.println("    - 16 个线程大概率操作不同桶，基本无锁竞争");
    }

    /**
     * 测试 Map 并发性能
     */
    private static long testMapPerformance(
        final Map<String, Integer> map,
        int threadCount,
        int operationsPerThread
    ) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = "Thread-" + threadId + "-Key-" + j;
                        // 50% 写，50% 读
                        map.put(key, j);
                        map.get(key);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();
        executor.shutdown();
        return endTime - startTime;
    }

    /**
     * size() 方法原理对比
     */
    private static void demonstrateSizeMethod() {
        System.out.println("\n【size() 方法原理演进】");
        System.out.println("========================================");

        System.out.println("\n【JDK7 size() 实现】");
        System.out.println("----------------------------------------");
        System.out.println("原理：累加所有 Segment 的 count");
        System.out.println();
        System.out.println("int size() {");
        System.out.println("    long sum = 0;");
        System.out.println("    for (Segment seg : segments) {");
        System.out.println("        sum += seg.count;");
        System.out.println("    }");
        System.out.println("    return (int) sum;");
        System.out.println("}");
        System.out.println();
        System.out.println("问题：");
        System.out.println("  - 需要遍历所有 Segment");
        System.out.println("  - 多个 Segment 的 count 可能变化，不精确");
        System.out.println("  - 如果在迭代过程中 map 被修改，结果不准确");

        System.out.println("\n【JDK8 size() 实现】");
        System.out.println("----------------------------------------");
        System.out.println(
            "原理：baseCount + CounterCell[] 分散计数（类似 LongAdder）"
        );
        System.out.println();
        System.out.println("结构：");
        System.out.println("  baseCount：无竞争时的计数（volatile long）");
        System.out.println("  CounterCell[]：竞争时的分散计数");
        System.out.println("    ├── Cell 0: thread 0-3 的计数值");
        System.out.println("    ├── Cell 1: thread 4-7 的计数值");
        System.out.println("    └── Cell n: ...");
        System.out.println();
        System.out.println("计算：");
        System.out.println("  sum = baseCount + sum(CounterCell[])");
        System.out.println();
        System.out.println("优势：");
        System.out.println("  - 无锁更新：使用 CAS 操作");
        System.out.println("  - 分散热点：不同线程操作不同 Cell");
        System.out.println("  - 最终一致性：弱一致性，非实时精确");

        // 演示
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        for (int i = 0; i < 1000; i++) {
            map.put("Key" + i, "Value" + i);
        }
        System.out.println("\n【实际演示】");
        System.out.println("插入 1000 个元素，size() = " + map.size());
    }

    /**
     * 原子操作演示
     */
    private static void demonstrateAtomicOperations() {
        System.out.println("\n【原子操作演示】");
        System.out.println("========================================");

        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        map.put("counter", 0);

        System.out.println("\n1. putIfAbsent（不存在才插入）：");
        System.out.println(
            "   putIfAbsent('counter', 100) 返回旧值: " +
                map.putIfAbsent("counter", 100)
        );
        System.out.println("   counter 仍为: " + map.get("counter"));

        System.out.println("\n2. replace（CAS 风格替换）：");
        System.out.println(
            "   replace('counter', 0, 10): " + map.replace("counter", 0, 10)
        );
        System.out.println("   counter 变为: " + map.get("counter"));

        System.out.println("\n3. compute（原子计算）：");
        map.compute("counter", (k, v) -> v == null ? 0 : v * 2);
        System.out.println("   compute('counter', x -> x * 2)");
        System.out.println("   counter = " + map.get("counter"));

        System.out.println("\n4. computeIfAbsent（不存在则计算）：");
        map.computeIfAbsent("newKey", k -> k.length() * 10);
        System.out.println(
            "   computeIfAbsent('newKey', k -> k.length() * 10)"
        );
        System.out.println("   newKey = " + map.get("newKey"));

        System.out.println("\n5. merge（合并操作）：");
        map.merge("counter", 5, Integer::sum); // 10 + 5 = 15
        System.out.println("   merge('counter', 5, Integer::sum)");
        System.out.println("   counter = " + map.get("counter"));
    }

    /**
     * 并发场景演示
     */
    private static void demonstrateConcurrentScenario() throws Exception {
        System.out.println("\n【并发场景：计数器统计】");
        System.out.println("========================================");

        final ConcurrentHashMap<String, AtomicInteger> counterMap =
            new ConcurrentHashMap<>();

        int threadCount = 10;
        int incrementsPerThread = 10000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        // 原子更新操作
                        counterMap.compute("total", (k, v) -> {
                            if (v == null) {
                                return new AtomicInteger(1);
                            }
                            v.incrementAndGet();
                            return v;
                        });
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        int totalCount = counterMap.get("total").get();
        int expectedCount = threadCount * incrementsPerThread;

        System.out.println("10 个线程，每个执行 10000 次累加");
        System.out.println("预期值: " + expectedCount);
        System.out.println("实际值: " + totalCount);
        System.out.println(
            "结果: " + (totalCount == expectedCount ? "正确" : "错误")
        );

        System.out.println("\n【关键点】");
        System.out.println("compute() 方法保证原子性：");
        System.out.println("  - 读取、计算、更新三个操作原子化");
        System.out.println("  - 使用 synchronized 锁住桶头节点");
        System.out.println("  - 防止并发修改导致数据丢失");
    }
}

/**
 * ConcurrentHashMap 实现原理总结
 *
 * 【JDK7 实现】
 * 数据结构：Segment[16] + HashEntry[][]
 * 锁机制：ReentrantLock（Segment级别）
 * 特点：
 *   - 最多支持16个线程并发写（默认）
 *   - 读操作无锁（volatile）
 *   - size计算需要遍历
 *
 * 【JDK8 实现】
 * 数据结构：Node[] + 链表/红黑树
 * 锁机制：CAS + synchronized（桶级锁）
 * 特点：
 *   - 读操作完全无锁
 *   - CAS操作空桶插入
 *   - synchronized锁链表头节点
 *   - CounterCell分散size计算
 *   - 多线程协助扩容
 *   - 树化优化长链
 *
 * 【演进原因】
 * 1. synchronized性能提升（JDK6+锁优化）
 * 2. 实现更简单，维护更容易
 * 3. 更高的并发度（桶级锁）
 * 4. 更好的性能优化（无锁读、CounterCell）
 *
 * 【使用建议】
 * - 高并发首选（代替Hashtable和synchronizedMap）
 * - 读多写少性能极佳
 * - 合理使用原子方法（compute、merge等）
 * - 注意size()的弱一致性
 */
