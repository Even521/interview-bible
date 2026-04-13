# HashMap 面试题

## 目录
1. [基础概念](#基础概念)
2. [底层数据结构](#底层数据结构)
3. [JDK7 vs JDK8 对比](#jdk7-vs-jdk8-对比)
4. [put 方法源码分析](#put-方法源码分析)
5. [扩容机制](#扩容机制)
6. [哈希算法](#哈希算法)
7. [线程安全问题](#线程安全问题)
8. [与其他 Map 对比](#与其他-map-对比)
9. [代码示例](../code/HashMapDemo.java)

---

## 基础概念

### 1. 什么是 HashMap？它的特点是什么？

**答案：**

HashMap 是 Java 中最常用的集合类之一，它基于**哈希表**实现，提供 key-value 的存储和快速查找能力。

**核心特点：**

| 特性 | 说明 |
|------|------|
| 键值对存储 | 每个元素包含一个键和一个值 |
| 键唯一 | 不允许重复的键（重复 put 会覆盖旧值） |
| 允许 null | 允许一个 null 键和多个 null 值 |
| 无序 | 不保证元素的顺序 |
| 非线程安全 | 多线程环境下需要外部同步 |
| 时间复杂度 | 平均 O(1)，最坏 O(log n)（JDK8+） |

**使用示例：**
```java
Map<String, Integer> map = new HashMap<>();
map.put("Alice", 25);
map.put("Bob", 30);

Integer age = map.get("Alice"); // O(1)
```

---

## 底层数据结构

### 2. HashMap 的底层数据结构是什么？

**答案：**

JDK 8 中 HashMap 采用 **"数组 + 链表 + 红黑树"** 的复合结构。

**结构图示：**
```
数组（Node[] table）
├── 索引0 → null
├── 索引1 → Node(key1, value1) → Node(key2, value2) → TreeNode（如果链表长度≥8且数组长度≥64）
├── 索引2 → null
└── 索引n → Node(key3, value3)
```

**节点定义：**
```java
// JDK 8 Node 定义
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;    // 哈希值
    final K key;       // 键
    V value;           // 值
    Node<K,V> next;    // 下一个节点（链表指针）
}

// 红黑树节点
static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {
    TreeNode<K,V> parent;  // 父节点
    TreeNode<K,V> left;    // 左子节点
    TreeNode<K,V> right;   // 右子节点
    TreeNode<K,V> prev;    // 前驱节点（用于删除）
    boolean red;           // 颜色标记（红/黑）
}
```

---

## JDK7 vs JDK8 对比

### 3. JDK7 和 JDK8 中 HashMap 有什么区别？

**答案：**

| 特性 | JDK 7 | JDK 8 |
|------|-------|-------|
| **数据结构** | 数组 + 链表 | 数组 + 链表 + 红黑树 |
| **插入方式** | 头插法（导致死循环） | 尾插法（避免死循环） |
| **扩容策略** | 先扩容后插入 | 先插入后扩容 |
| **哈希计算** | 4次位运算 + 5次异或 | 1次位运算 + 1次异或 |
| **链表转树阈值** | 无 | 链表长度≥8 且 数组长度≥64 |

### 4. JDK8 为什么引入红黑树？

**答案：**

**核心原因：解决哈希冲突导致的性能退化问题**

**问题背景：**
```java
// 理想情况：哈希分布均匀，每个索引只有一个元素
// 时间复杂度：O(1)

// 极端情况：所有 key 的哈希值相同，都落到同一个索引
// 形成一个长链表
// 时间复杂度：O(n) - 退化为线性查找
```

**引入红黑树的收益：**

| 场景 | JDK7（纯链表） | JDK8（链表+红黑树） |
|------|----------------|---------------------|
| 短链表（<8） | O(1) ~ O(8) | O(1) ~ O(8) |
| 长链表（≥8） | O(n) 性能差 | O(log n) 性能好 |
| 极端哈希冲突 | 可能 O(10000+) | O(log 10000) ≈ O(14) |

**转换阈值的选择：**

```java
// 链表转红黑树的阈值：8
static final int TREEIFY_THRESHOLD = 8;

// 红黑树转链表的阈值：6
static final int UNTREEIFY_THRESHOLD = 6;

// 最小树化容量：64
static final int MIN_TREEIFY_CAPACITY = 64;
```

**为什么是 8？**
- 根据**泊松分布**统计，哈希碰撞达到 8 的概率已经非常低（0.00000606%）
- 小于 8 时链表性能更好（维护简单，内存占用少）
- 大于 8 时红黑树性能优势明显

**为什么需要数组长度≥64才树化？**
- 优先扩容来解决哈希冲突
- 如果数组太小，应该通过扩容来分散节点
- 避免在哈希表较小时就进行昂贵的树化操作

---

## put 方法源码分析

### 5. HashMap 的 put() 方法执行流程是什么？

**答案：**

```java
public V put(K key, V value) {
    return putVal(hash(key), key, value, false, true);
}
```

**详细流程（JDK 8）：**

```
1. 计算 key 的哈希值
   hash = (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16)

2. 计算数组下标
   index = (n - 1) & hash

3. 检查该位置是否为空
   ├─ 为空：直接创建新节点放入
   └─ 不为空：遍历链表/红黑树
      ├─ 存在相同 key：覆盖旧值
      └─ 不存在：尾插法插入新节点
         └─ 检查是否需要树化（链表长度≥8）

4. 检查是否需要扩容
   ++size > threshold（容量 * 负载因子 0.75）
   └─ 需要：扩容为原来的 2 倍，重新哈希
```

**源码简化版：**
```java
final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    
    // 1. 数组未初始化或长度为0，先扩容
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;
    
    // 2. 计算索引，该位置为空，直接插入
    if ((p = tab[i = (n - 1) & hash]) == null)
        tab[i] = newNode(hash, key, value, null);
    
    else {
        Node<K,V> e; K k;
        
        // 3. 第一个节点key相同，覆盖
        if (p.hash == hash &&
            ((k = p.key) == key || (key != null && key.equals(k))))
            e = p;
        
        // 4. 是红黑树节点
        else if (p instanceof TreeNode)
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
        
        // 5. 链表遍历
        else {
            for (int binCount = 0; ; ++binCount) {
                if ((e = p.next) == null) {
                    // 尾插法
                    p.next = newNode(hash, key, value, null);
                    
                    // 检查是否需要树化
                    if (binCount >= TREEIFY_THRESHOLD - 1)
                        treeifyBin(tab, hash);
                    break;
                }
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    break;
                p = e;
            }
        }
        
        // 6. 覆盖旧值
        if (e != null) {
            V oldValue = e.value;
            if (!onlyIfAbsent || oldValue == null)
                e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
    }
    
    // 7. 检查扩容
    if (++size > threshold)
        resize();
    afterNodeInsertion(evict);
    return null;
}
```

---

## 扩容机制

### 6. HashMap 什么时候扩容？扩容过程是怎样的？

**答案：**

**扩容触发条件：**
```java
// 当元素数量 > 容量 × 负载因子（默认 0.75）时触发扩容
if (++size > threshold) {
    resize();
}
```

**扩容流程（JDK 8）：**

```
1. 新容量 = 旧容量 × 2
2. 创建新数组
3. 将旧数组数据迁移到新数组
   ├─ 单个节点：直接重新计算索引放入
   ├─ 链表：拆分成两个链表（高位和低位）
   └─ 红黑树：拆分成两个树，如果树太小转回链表
4. 更新 threshold
```

**为什么 JDK8 拆分成高低位链表？**

```java
// 原来计算索引：(n - 1) & hash
// n=16 时：n-1 = 15 = 0000 1111
// n=32 时：n-1 = 31 = 0001 1111

// 扩容后，只需要看 hash 的第 5 位（新增的高位）
// 如果为0：索引不变
// 如果为1：索引 = 原索引 + 16
```

**JDK 8 优化：** 不需要重新计算哈希，只需要判断 hash 的某一位是 0 还是 1。

---

## 哈希算法

### 7. HashMap 如何计算哈希值？为什么要这样设计？

**答案：**

**JDK 8 的 hash 方法：**
```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

**设计原理：**

```
原始 hashCode：  1111 0000 1111 0000 1111 0000 1111 0000
右移16位：       0000 0000 0000 0000 1111 0000 1111 0000
异或结果：       1111 0000 1111 0000 0000 0000 0000 0000

高16位与低16位混合，让哈希值更分散
```

**为什么要右移 16 位异或？**

```java
// 计算索引时：(n - 1) & hash
// n 是 2 的幂，n-1 二进制全是 1
// 如 n=16, n-1=15 = 0000 0000 0000 1111

// 如果直接用 hashCode，高位信息被忽略
// 只使用了低 4 位，容易发生哈希冲突

// 解决方案：让高位也参与运算
// hashCode 高16位与低16位异或，混合高位信息
```

**效果：**
- 减少哈希冲突
- 让元素分布更均匀
- 提高查询效率

---

## 线程安全问题

### 8. HashMap 为什么是线程不安全的？

**答案：**

**并发场景下的问题：**

| 问题 | JDK7 | JDK8 |
|------|------|------|
| **死循环** | 有（并发扩容） | 无（尾插法） |
| **数据丢失** | 有 | 有 |
| **size 不准确** | 有 | 有 |

**JDK7 死循环原因（头插法）：**
```java
// 并发扩容时，两个线程同时操作链表
// 线程A：A.next = B, B.next = A（形成环）
// 线程B：遍历链表时陷入死循环
```

**JDK8 的改进：**
- 使用尾插法，避免了死循环
- 但仍存在并发 put 导致数据丢失的问题

**解决方案：**
```java
// 方式1：使用 Collections.synchronizedMap
Map<String, String> syncMap = Collections.synchronizedMap(new HashMap<>());

// 方式2：使用 ConcurrentHashMap（推荐）
Map<String, String> concurrentMap = new ConcurrentHashMap<>();

// 方式3：使用 Hashtable（不推荐，全表锁性能差）
Map<String, String> hashTable = new Hashtable<>();
```

---

## 与其他 Map 对比

### 9. HashMap vs Hashtable vs ConcurrentHashMap？

**答案：**

| 特性 | HashMap | Hashtable | ConcurrentHashMap |
|------|---------|-----------|-------------------|
| 线程安全 | ❌ | ✅ | ✅ |
| 性能 | 高 | 低（全表锁） | 高（分段锁/JDK8 CAS） |
| null 键值 | 允许 | 不允许 | 不允许 |
| 迭代器 | fail-fast | fail-fast | 弱一致性 |
| 出现版本 | JDK 1.2 | JDK 1.0 | JDK 5 |

**ConcurrentHashMap 的并发优化：**

JDK 7：分段锁（Segment）
```
Segment[16]
├── Segment 0: HashEntry[]
├── Segment 1: HashEntry[]
└── 每个 Segment 独立加锁
```

JDK 8：CAS + synchronized（细粒度锁）
```
数组每个桶独立加锁
只有发生哈希冲突时才加锁
读操作无锁（volatile）
```

### 10. HashMap vs LinkedHashMap vs TreeMap？

**答案：**

| 特性 | HashMap | LinkedHashMap | TreeMap |
|------|---------|-----------------|---------|
| 有序性 | 无序 | 插入/访问顺序 | 键的自然/自定义顺序 |
| 实现 | 哈希表 | 哈希表 + 双向链表 | 红黑树 |
| 时间复杂度 | O(1) | O(1) | O(log n) |
| 内存占用 | 少 | 较多（维护链表） | 最多 |
| 使用场景 | 通用 | LRU Cache | 需要排序的场景 |

---

## 高级话题

### 11. 为什么 HashMap 容量必须是 2 的幂？

**答案：**

**三个原因：**

1. **位运算替代取模**
```java
// hash % n 等价于 hash & (n-1)
// 但必须 n 是 2 的幂

// 如 n=16, n-1=15=1111
// hash        : 1010 1010
// n-1         : 0000 1111
// hash & (n-1): 0000 1010 = 10
```

2. **扩容时方便重新哈希**
```java
// 扩容后元素要么在原位置，要么在 原位置+旧容量
// 只需判断 hash 的某一位是 0 还是 1
```

3. **分布更均匀**
```java
// n-1 的二进制全是 1，& 运算能充分利用 hash 值的每一位
```

### 12. HashMap 为什么用 0.75 作为负载因子？

**答案：**

**权衡空间和时间：**

| 负载因子 | 空间占用 | 冲突概率 | 查询效率 |
|---------|---------|---------|---------|
| 0.5 | 多（浪费空间） | 低 | 快 |
| **0.75** | 适中 | 适中 | **均衡** |
| 1.0 | 少（充分利用） | 高 | 慢 |

**泊松分布验证：**
```java
// 0.75 是时间和空间成本上的权衡
// 根据统计，当达到 0.75 时，链表的平均长度为 0.5
// 既减少了哈希冲突，又不浪费太多空间
```

---

## 面试技巧

### 高频面试题 TOP 10

1. **HashMap 的底层数据结构？JDK8 为什么引入红黑树？**
2. **HashMap put 方法的执行流程？**
3. **JDK7 和 JDK8 中 HashMap 的区别？**
4. **HashMap 扩容机制？为什么扩容是 2 倍？**
5. **HashMap 的 hash 算法怎么设计的？为什么要这样设计？**
6. **HashMap 为什么线程不安全？JDK7 为什么会有死循环？**
7. **HashMap、Hashtable、ConcurrentHashMap 的区别？**
8. **为什么 HashMap 容量必须是 2 的幂？**
9. **链表和红黑树的转换阈值为什么是 8？**
10. **HashMap 如何实现线程安全？**

### 面试答题模板

**问：JDK8 为什么引入红黑树？**

**答：**

> JDK8 引入红黑树是为了**解决哈希冲突导致的性能退化**问题。
>
> 在 JDK7 中，HashMap 使用"数组+链表"结构。当发生哈希冲突时，元素会形成链表。**极端情况下**，如果所有元素哈希值都相同，链表会很长，查询时间复杂度退化为 **O(n)**。
>
> JDK8 引入了**红黑树**优化：当链表长度 ≥ 8 且数组长度 ≥ 64 时，链表会转换为红黑树。红黑树的查询时间复杂度为 **O(log n)**，即使在极端哈希冲突场景下也能保持较好的性能。
>
> 选择 8 作为阈值的原因是：
> 1. 根据**泊松分布**统计，哈希碰撞达到 8 的概率已经非常低（0.00000606%）
> 2. 小于 8 时链表性能更好（维护简单、内存占用少）
> 3. 大于 8 时红黑树性能优势明显
>
> 此外，JDK8 还做了其他优化：使用**尾插法**代替头插法（避免并发扩容死循环）、**高低位链表**优化扩容等。

---

*本文档持续更新，建议收藏备用*