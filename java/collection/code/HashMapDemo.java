import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * HashMap 底层实现原理演示
 *
 * 1. 基本使用
 * 2. 哈希冲突演示
 * 3. JDK8 链表转红黑树
 * 4. 扩容机制
 * 5. 简单的 HashMap 实现
 *
 * @author Java面试宝典
 */
public class HashMapDemo {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("HashMap 底层实现原理演示");
        System.out.println("========================================\n");

        // 1. 基本使用演示
        demonstrateBasicUsage();

        // 2. 哈希冲突演示
        demonstrateHashCollision();

        // 3. 红黑树转换演示（JDK8特性）
        demonstrateTreeify();

        // 4. 扩容机制演示
        demonstrateResize();

        // 5. 自定义简单 HashMap 实现
        demonstrateCustomHashMap();

        System.out.println("\n========================================");
        System.out.println("演示结束！");
        System.out.println("========================================");
    }

    /**
     * 演示 HashMap 基本使用
     */
    private static void demonstrateBasicUsage() {
        System.out.println("【演示1】HashMap 基本使用");
        System.out.println("----------------------------------------");

        Map<String, Integer> map = new HashMap<>(16, 0.75f);

        // 添加元素
        map.put("Alice", 25);
        map.put("Bob", 30);
        map.put("Charlie", 35);

        System.out.println("添加元素：" + map);

        // 获取元素
        System.out.println("Alice 的年龄：" + map.get("Alice"));

        // 更新元素
        map.put("Alice", 26);
        System.out.println("更新后 Alice 的年龄：" + map.get("Alice"));

        // 遍历
        System.out.println("遍历 HashMap：");
        map.forEach((key, value) ->
            System.out.println("  Key=" + key + ", Value=" + value)
        );

        // 展示 HashMap 的内部结构信息
        System.out.println("HashMap 大小：" + map.size());
        System.out.println();
    }

    /**
     * 演示哈希冲突
     * 通过特殊构造的键，让它们产生相同的哈希值或落在同一个桶中
     */
    private static void demonstrateHashCollision() {
        System.out.println("【演示2】哈希冲突演示");
        System.out.println("----------------------------------------");

        // 手动计算哈希碰撞
        // 两个不同的对象，经过 hashCode() 和 hash() 计算后可能落在同一个桶

        Map<KeyWithSameHash, String> map = new HashMap<>(4, 100); // 小容量，高负载因子，容易冲突

        // 创建多个产生相同哈希值的键
        KeyWithSameHash key1 = new KeyWithSameHash("Key1");
        KeyWithSameHash key2 = new KeyWithSameHash("Key2");
        KeyWithSameHash key3 = new KeyWithSameHash("Key3");

        map.put(key1, "Value1");
        map.put(key2, "Value2");
        map.put(key3, "Value3");

        System.out.println("Key1 hashCode: " + key1.hashCode());
        System.out.println("Key2 hashCode: " + key2.hashCode());
        System.out.println("Key3 hashCode: " + key3.hashCode());
        System.out.println("这三个键会产生哈希冲突");

        System.out.println("获取 Key1: " + map.get(key1));
        System.out.println("获取 Key2: " + map.get(key2));
        System.out.println("获取 Key3: " + map.get(key3));
        System.out.println();

        // 解释哈希冲突解决机制
        System.out.println("【HashMap 如何解决哈希冲突？】");
        System.out.println("  1. JDK7：数组 + 链表（头插法）");
        System.out.println("  2. JDK8：数组 + 链表/红黑树（尾插法）");
        System.out.println("     - 链表长度 >= 8 且数组长度 >= 64 时转红黑树");
        System.out.println("     - 红黑树节点 <= 6 时退化为链表");
        System.out.println();
    }

    /**
     * 演示链表转红黑树
     * 触发条件：链表长度 >= 8 且数组长度 >= 64
     */
    private static void demonstrateTreeify() {
        System.out.println("【演示3】链表转红黑树（JDK8优化）");
        System.out.println("----------------------------------------");

        System.out.println("触发链表转红黑树的条件：");
        System.out.println("  1. 链表长度 >= TREEIFY_THRESHOLD (8)");
        System.out.println("  2. 数组长度 >= MIN_TREEIFY_CAPACITY (64)");
        System.out.println();

        Map<KeyWithSameHash, String> map = new HashMap<>();

        System.out.println("准备插入 12 个哈希冲突的键：");
        for (int i = 0; i < 12; i++) {
            KeyWithSameHash key = new KeyWithSameHash("ConflictKey-" + i);
            map.put(key, "Value-" + i);
            System.out.println("  插入 Key-" + i + ", map.size=" + map.size());
        }

        System.out.println();
        System.out.println("【为什么 JDK8 引入红黑树？】");
        System.out.println("  JDK7 链表查询时间复杂度：O(n)");
        System.out.println("  JDK8 红黑树查询时间复杂度：O(log n)");
        System.out.println(
            "  极端情况下（大量哈希冲突），性能从 O(n) 提升到 O(log n)"
        );
        System.out.println();
    }

    /**
     * 演示扩容机制
     */
    private static void demonstrateResize() {
        System.out.println("【演示4】扩容机制");
        System.out.println("----------------------------------------");

        System.out.println("扩容条件：");
        System.out.println("  size > capacity * loadFactor");
        System.out.println("  默认：capacity=16, loadFactor=0.75");
        System.out.println("  触发阈值：16 * 0.75 = 12");
        System.out.println();

        // 创建指定初始容量的小容量 HashMap，便于观察扩容
        Map<String, String> map = new HashMap<>(4, 0.75f);

        System.out.println("初始容量：4，负载因子：0.75，扩容阈值：3");
        System.out.println();

        for (int i = 0; i < 10; i++) {
            String key = "Key" + i;
            map.put(key, "Value" + i);
            System.out.println("  插入 " + key + ", size=" + map.size());
        }

        System.out.println();
        System.out.println("【JDK8 扩容优化】");
        System.out.println("  JDK8 使用 (hash & oldCap) == 0 判断节点位置：");
        System.out.println("    - 结果为 0：节点保持在新数组的原位置");
        System.out.println(
            "    - 结果为 1：节点移动到新数组的 原位置 + oldCap 位置"
        );
        System.out.println("  优点：避免了 JDK7 中重新计算哈希值的过程");
        System.out.println();
    }

    /**
     * 演示自定义的简单 HashMap 实现
     */
    private static void demonstrateCustomHashMap() {
        System.out.println("【演示5】自定义简单 HashMap 实现");
        System.out.println("----------------------------------------");

        SimpleHashMap<String, Integer> map = new SimpleHashMap<>();

        System.out.println("插入元素：");
        map.put("Alice", 25);
        map.put("Bob", 30);
        map.put("Charlie", 35);
        map.put("David", 40);

        System.out.println("获取元素：");
        System.out.println("  Alice = " + map.get("Alice"));
        System.out.println("  Bob = " + map.get("Bob"));
        System.out.println("  Charlie = " + map.get("Charlie"));

        System.out.println("当前大小：" + map.size());
        System.out.println();
    }

    /**
     * 自定义键类，用于产生哈希冲突
     */
    static class KeyWithSameHash {

        private final String value;

        public KeyWithSameHash(String value) {
            this.value = value;
        }

        /**
         * 重写 hashCode，让所有实例返回相同的哈希值
         * 从而强制产生哈希冲突
         */
        @Override
        public int hashCode() {
            return 12345; // 固定的哈希值，产生冲突
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            KeyWithSameHash that = (KeyWithSameHash) obj;
            return Objects.equals(value, that.value);
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * 简化的 HashMap 实现（仅用于演示原理）
     *
     * HashMap 核心组成：
     * 1. Node[] table：数组，每个元素是一个桶
     * 2. Node：节点，存储 key-value 和下一个节点引用
     * 3. hash：key 的哈希值
     * 4. 冲突解决：链地址法
     */
    static class SimpleHashMap<K, V> {

        /**
         * 默认初始化容量
         */
        static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // 16

        /**
         * 默认负载因子
         */
        static final float DEFAULT_LOAD_FACTOR = 0.75f;

        /**
         * 链表转红黑树阈值
         */
        static final int TREEIFY_THRESHOLD = 8;

        /**
         * Node 节点类
         * 存储 key-value 对
         */
        static class Node<K, V> {

            final int hash; // 哈希值
            final K key; // 键
            V value; // 值
            Node<K, V> next; // 下一个节点

            Node(int hash, K key, V value, Node<K, V> next) {
                this.hash = hash;
                this.key = key;
                this.value = value;
                this.next = next;
            }

            public final K getKey() {
                return key;
            }

            public final V getValue() {
                return value;
            }

            public final String toString() {
                return key + "=" + value;
            }
        }

        // 存储数据的数组
        Node<K, V>[] table;

        // 元素数量
        int size;

        // 扩容阈值
        int threshold;

        // 负载因子
        final float loadFactor;

        public SimpleHashMap() {
            this.loadFactor = DEFAULT_LOAD_FACTOR;
            this.threshold = (int) (DEFAULT_INITIAL_CAPACITY *
                DEFAULT_LOAD_FACTOR);
        }

        /**
         * 计算 key 的哈希值
         * 与 JDK8 中的 hash 方法一致
         */
        static int hash(Object key) {
            int h;
            return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
        }

        /**
         * 获取节点
         */
        public V get(Object key) {
            Node<K, V> e;
            return (e = getNode(hash(key), key)) == null ? null : e.value;
        }

        /**
         * 获取节点实现
         */
        final Node<K, V> getNode(int hash, Object key) {
            Node<K, V>[] tab;
            Node<K, V> first, e;
            int n;
            K k;

            // table 不为空且长度大于0，且对应桶不为空
            if (
                (tab = table) != null &&
                (n = tab.length) > 0 &&
                (first = tab[(n - 1) & hash]) != null
            ) {
                // 检查第一个节点
                if (
                    first.hash == hash &&
                    ((k = first.key) == key || (key != null && key.equals(k)))
                ) {
                    return first;
                }

                // 遍历链表
                if ((e = first.next) != null) {
                    do {
                        if (
                            e.hash == hash &&
                            ((k = e.key) == key ||
                                (key != null && key.equals(k)))
                        ) {
                            return e;
                        }
                    } while ((e = e.next) != null);
                }
            }
            return null;
        }

        /**
         * 添加元素
         */
        public V put(K key, V value) {
            return putVal(hash(key), key, value, false, true);
        }

        /**
         * put 核心实现
         */
        final V putVal(
            int hash,
            K key,
            V value,
            boolean onlyIfAbsent,
            boolean evict
        ) {
            Node<K, V>[] tab;
            Node<K, V> p;
            int n, i;

            // 1. 如果 table 为空或长度为0，初始化
            if ((tab = table) == null || (n = tab.length) == 0) {
                n = (tab = resize()).length;
            }

            // 2. 计算桶的位置并检查是否为空
            if ((p = tab[i = (n - 1) & hash]) == null) {
                // 该桶为空，直接插入新节点
                tab[i] = new Node<>(hash, key, value, null);
            } else {
                // 桶不为空，处理哈希冲突
                Node<K, V> e;
                K k;

                // 2.1 检查第一个节点是否是同一个 key
                if (
                    p.hash == hash &&
                    ((k = p.key) == key || (key != null && key.equals(k)))
                ) {
                    e = p;
                } else {
                    // 2.2 遍历链表查找
                    while (true) {
                        if ((e = p.next) == null) {
                            // 链表尾部，插入新节点
                            p.next = new Node<>(hash, key, value, null);
                            break;
                        }
                        if (
                            e.hash == hash &&
                            ((k = e.key) == key ||
                                (key != null && key.equals(k)))
                        ) {
                            // 找到相同的 key
                            break;
                        }
                        p = e;
                    }
                }

                // 2.3 如果 key 已存在，更新 value
                if (e != null) {
                    V oldValue = e.value;
                    if (!onlyIfAbsent || oldValue == null) {
                        e.value = value;
                    }
                    return oldValue;
                }
            }

            // 3. 增加 size，检查是否需要扩容
            if (++size > threshold) {
                resize();
            }

            System.out.println(
                "  put(" +
                    key +
                    ", " +
                    value +
                    ") -> hash=" +
                    hash +
                    ", bucket=" +
                    i +
                    ", size=" +
                    size
            );
            return null;
        }

        /**
         * 扩容方法
         */
        final Node<K, V>[] resize() {
            Node<K, V>[] oldTab = table;
            int oldCap = (oldTab == null) ? 0 : oldTab.length;
            int oldThr = threshold;
            int newCap,
                newThr = 0;

            if (oldCap > 0) {
                // 超过最大容量，不再扩容
                if (oldCap >= (1 << 30)) {
                    threshold = Integer.MAX_VALUE;
                    return oldTab;
                }
                // 容量翻倍
                else if (
                    (newCap = oldCap << 1) < (1 << 30) &&
                    oldCap >= DEFAULT_INITIAL_CAPACITY
                ) {
                    newThr = oldThr << 1; // 阈值翻倍
                }
            } else {
                // 初始化
                newCap = DEFAULT_INITIAL_CAPACITY;
                newThr = (int) (DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
            }

            threshold = newThr;

            @SuppressWarnings({ "rawtypes", "unchecked" })
            Node<K, V>[] newTab = (Node<K, V>[]) new Node[newCap];
            table = newTab;

            // 移动旧节点到新数组
            if (oldTab != null) {
                for (int j = 0; j < oldCap; ++j) {
                    Node<K, V> e;
                    if ((e = oldTab[j]) != null) {
                        oldTab[j] = null;
                        if (e.next == null) {
                            // 只有一个节点，直接移动
                            newTab[e.hash & (newCap - 1)] = e;
                        } else {
                            // 有多个节点，需要重新分配
                            Node<K, V> loHead = null,
                                loTail = null;
                            Node<K, V> hiHead = null,
                                hiTail = null;
                            Node<K, V> next;

                            do {
                                next = e.next;
                                // JDK8 优化：判断节点在新数组中的位置
                                if ((e.hash & oldCap) == 0) {
                                    // 保持原位置
                                    if (loTail == null) {
                                        loHead = e;
                                    } else {
                                        loTail.next = e;
                                    }
                                    loTail = e;
                                } else {
                                    // 移动到新位置：原位置 + oldCap
                                    if (hiTail == null) {
                                        hiHead = e;
                                    } else {
                                        hiTail.next = e;
                                    }
                                    hiTail = e;
                                }
                            } while ((e = next) != null);

                            if (loTail != null) {
                                loTail.next = null;
                                newTab[j] = loHead;
                            }
                            if (hiTail != null) {
                                hiTail.next = null;
                                newTab[j + oldCap] = hiHead;
                            }
                        }
                    }
                }
            }

            System.out.println(
                "  【扩容】 oldCap=" + oldCap + ", newCap=" + newCap
            );
            return newTab;
        }

        public int size() {
            return size;
        }
    }
}
