# ConcurrentHashMap 面试题

## 目录
1. [JDK7 vs JDK8 实现对比](#jdk7-vs-jdk8-实现对比)
2. [分段锁到 CAS 的演进](#分段锁到-cas-的演进)
3. [核心源码分析](#核心源码分析)
4. [性能对比](#性能对比)
5. [线程安全实现](#线程安全实现)
6. [代码示例](../code/ConcurrentHashMapDemo.java)

---

## JDK7 vs JDK8 实现对比

### 1. JDK7 分段锁（Segment）实现原理？

**答案：**

**数据结构：**
```
ConcurrentHashMap (JDK7)
└── Segment[16] (默认16个分段，每段一个ReentrantLock)
    ├── Segment 0: HashEntry[]
    ├── Segment 1: HashEntry[]
    └── Segment 15: HashEntry[]
```

**核心参数：**
```java
// 默认 Segment 数量（并发级别）
static final int DEFAULT_CONCURRENCY_LEVEL = 16;

// Segment 继承 ReentrantLock
static final class Segment<K,V> extends ReentrantLock implements Serializable {
    transient volatile HashEntry<K,V>[] table;
    transient int count;  // 该 Segment 中的元素数量
}
```

**put 流程：**
```java
// 1. 计算 hash，确定 Segment 索引
int segmentIndex = (hash >>> segmentShift) & segmentMask;

// 2. 获取 Segment 的锁
Segment<K,V> s = segments[segmentIndex];
s.lock();

// 3. 在 Segment 内定位 HashEntry
int index = hash & (s.table.length - 1);

// 4. 遍历链表，key存在则覆盖，否则插入
// 5. 释放锁
s.unlock();
```

**特点：**
- 最多支持 16 个线程并发写（默认）
- 读操作无需锁（volatile 保证可见性）
- Segment 数组不可扩容，初始化后大小固定
- size() 计算需要遍历所有 Segment

---

### 2. JDK8 CAS + synchronized 实现原理？

**答案：**

**数据结构变化：**
```
ConcurrentHashMap (JDK8)
└── Node[] table (直接用 Node 数组，无 Segment)
    ├── index 0 → null / Node / TreeBin(红黑树)
    ├── index 1 → Node → Node → TreeBin
    └── index n → Node → Node
```

**核心改进：**
1. **取消 Segment，直接使用 Node[] table**
2. **CAS 操作空桶插入**（无锁）
3. **synchronized 锁链表头节点**（细粒度锁）
4. **引入红黑树**（长链优化）
5. **多线程协助扩容**
6. **CounterCell 分散 size 计算**

**关键内部类：**
```java
// 链表节点
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    volatile V val;        // volatile保证可见性
    volatile Node<K,V> next;
}

// 红黑树
static final class TreeBin<K,V> extends Node<K,V> {
    TreeNode<K,V> root;    // 指向红黑树根节点
    volatile Thread waiter;
}

// 扩容标记节点
static final class ForwardingNode<K,V> extends Node<K,V> {
    // 标记该桶正在迁移到新table
}
```

**put 流程（JDK8）：**
```java
final V putVal(K key, V value, boolean onlyIfAbsent) {
    if (key == null || value == null) throw new NullPointerException();
    int hash = spread(key.hashCode());
    int binCount = 0;
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        // 1. table为空，CAS初始化
        if (tab == null || (n = tab.length) == 0)
            tab = initTable();
        
        // 2. 桶为空：CAS直接插入（无锁）
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            if (casTabAt(tab, i, null, new Node<K,V>(hash, key, value)))
                break;
        }
        
        // 3. 正在扩容：协助迁移
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);
        
        // 4. 桶非空：synchronized 锁住头节点
        else {
            V oldVal = null;
            synchronized (f) {  // 锁头节点！
                if (tabAt(tab, i) == f) {
                    if (fh >= 0) {  // 链表
                        binCount = 1;
                        for (Node<K,V> e = f;; ++binCount) {
                            K ek;
                            if (e.hash == hash &&
                                ((ek = e.key) == key ||
                                 (ek != null && key.equals(ek)))) {
                                oldVal = e.val;
                                if (!onlyIfAbsent)
                                    e.val = value;
                                break;
                            }
                            Node<K,V> pred = e;
                            if ((e = e.next) == null) {
                                pred.next = new Node<K,V>(hash, key, value);
                                break;
                            }
                        }
                    }
                    // ... 红黑树处理
                }
            }
            if (binCount != 0) {
                // 链表长度>=8，转为红黑树
                if (binCount >= TREEIFY_THRESHOLD)
                    treeifyBin(tab, i);
                if (oldVal != null)
                    return oldVal;
                break;
            }
        }
    }
    // 更新计数（CounterCell机制）
    addCount(1L, binCount);
    return null;
}
```

---

## 分段锁到 CAS 的演进

### 3. 为什么 JDK8 放弃分段锁，改用 CAS + synchronized？

**答案：**

**演进原因分析：**

| 方面 | JDK7 分段锁 | JDK8 CAS + synchronized |
|------|-------------|------------------------|
| **锁粒度** | Segment 级别（16个锁） | 桶级别（table.length个锁） |
| **并发度** | 固定（默认16） | 随扩容增加，理论无上限 |
| **读操作** | 无需锁（volatile） | 无需锁（volatile） |
| **空桶插入** | 需要获取Segment锁 | CAS 无锁插入 |
| **实现复杂度** | 高（维护Segment） | 低（直接操作数组） |
| **size()** | 遍历加锁，性能差 | CounterCell无锁计数 |

**核心原因：**

1. **synchronized 性能大幅提升（JDK6+）**
   - 引入偏向锁、轻量级锁、重量级锁升级机制
   - 大部分场景性能不逊于 ReentrantLock
   - 代码更简洁，JVM 优化更好

2. **细粒度锁比粗粒度锁更优**
   - JDK7：锁住整个 Segment（包含多个桶）
   - JDK8：只锁住单个桶（链表/红黑树头节点）
   - 更高的并发度，更少的锁竞争

3. **实现更简单，维护更容易**
   - Segment 机制复杂，代码量大
   - CAS + synchronized 逻辑清晰

4. **更好的性能优化空间**
   - 完全无锁的读操作
   - CounterCell 分散 size 计算热点
   - 多线程协助扩容

---

### 4. JDK8 的 size() 是怎么实现的？

**答案：**

**JDK7：** 累加所有 Segment 的 count
```java
public int size() {
    long sum = 0;
    for (Segment<K,V> seg : segments) {
        sum += seg.count;
    }
    return (int) sum;
}
// 问题：需要遍历，不精确，可能需要加锁
```

**JDK8：** CounterCell 分散计数（类似 LongAdder）
```java
// 结构
transient volatile long baseCount;           // 无竞争时的计数
transient volatile CounterCell[] counterCells; // 竞争时的分散计数

static final class CounterCell {
    volatile long value;  // 每个Cell独立计数
    CounterCell(long x) { value = x; }
}

public int size() {
    long n = sumCount();   // baseCount + sum(CounterCell[])
    return ((n < 0L) ? 0 : (n > (long)Integer.MAX_VALUE) ? 
            Integer.MAX_VALUE : (int)n);
}

final long sumCount() {
    CounterCell[] as = counterCells; CounterCell a;
    long sum = baseCount;
    if (as != null) {
        for (int i = 0; i < as.length; ++i) {
            if ((a = as[i]) != null)
                sum += a.value;
        }
    }
    return sum;
}
```

**优势：**
- 无锁更新（CAS）
- 分散热点（不同线程操作不同 Cell）
- 性能极高（弱一致性，非实时精确）

---

## 核心源码分析

### 5. JDK8 的 get() 方法为什么不需要加锁？

**答案：**

**源码：**
```java
public V get(Object key) {
    Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
    int h = spread(key.hashCode());
    
    // 1. table已初始化且桶不为空
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (e = tabAt(tab, (n - 1) & h)) != null) {
        
        // 2. 首节点匹配
        if ((eh = e.hash) == h) {
            if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                return e.val;
        }
        
        // 3. hash<0表示是红黑树或正在扩容
        else if (eh < 0)
            return (p = e.find(h, key)) != null ? p.val : null;
        
        // 4. 遍历链表
        while ((e = e.next) != null) {
            if (e.hash == h &&
                ((ek = e.key) == key || (ek != null && key.equals(ek))))
                return e.val;
        }
    }
    return null;
}
```

**无锁的原因：**
1. **val 和 next 都是 volatile**
   - `volatile V val`：保证值的可见性
   - `volatile Node<K,V> next`：保证链表的可见性

2. **get 只读不写**
   - 不涉及状态修改，无需同步
   - 即使遍历过程中链表被修改，也能正确读取（volatile保证）

3. **CAS 保证写操作的原子性**
   - 写操作使用 CAS，读操作不会读取到中间状态

---

### 6. JDK8 的扩容是如何支持多线程协助的？

**答案：**

**扩容触发条件：**
```java
// 元素数量超过阈值
if (s > (long)(sc = sizeCtl)) // sizeCtl > 0 表示阈值
    transfer(tab, null);
```

**多线程协助扩容：**
```java
private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
    int n = tab.length, stride;
    
    // 计算每个线程处理的桶数量（最小16个）
    if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
        stride = MIN_TRANSFER_STRIDE;
    
    // 初始化新table
    if (nextTab == null) {
        // ...
    }
    
    int nextn = nextTab.length;
    ForwardingNode<K,V> fwd = new ForwardingNode<K,V>(nextTab);
    
    // 多个线程从不同的桶位置开始迁移
    for (int i = 0, bound = 0;;) {
        Node<K,V> f; int fh;
        
        // 分配迁移任务给线程
        while (advance) {
            int nextIndex, nextBound;
            if (--i >= bound || finishing)
                advance = false;
            else if ((nextIndex = transferIndex) <= 0) {
                i = -1;
                advance = false;
            }
            else if (U.compareAndSwapInt
                     (this, TRANSFERINDEX, nextIndex,
                      nextBound = (nextIndex > stride ?
                                   nextIndex - stride : 0))) {
                bound = nextBound;
                i = nextIndex - 1;
                advance = false;
            }
        }
        
        // 迁移桶i的数据
        if (i < 0 || i >= n || i + n >= nextn) {
            // ... 完成判断
        }
        else if ((f = tabAt(tab, i)) == null)
            advance = casTabAt(tab, i, null, fwd);  // CAS标记为空桶
        else if ((fh = f.hash) == MOVED)
            advance = true;  // 其他线程已迁移
        else {
            synchronized (f) {  // 锁住该桶
                if (tabAt(tab, i) == f) {
                    Node<K,V> ln, hn;
                    if (fh >= 0) {  // 链表迁移
                        // ... 拆分为高低位链表
                    }
                    else if (f instanceof TreeBin) {  // 红黑树迁移
                        // ... 拆分红黑树
                    }
                }
            }
        }
    }
}
```

**关键点：**
1. **每个线程负责一段连续的桶**（默认16个）
2. **ForwardingNode 标记**：表示该桶已迁移
3. **线程安全**：每个桶的迁移使用 synchronized 锁

---

## 性能对比

### 7. ConcurrentHashMap vs Hashtable 性能对比？

**答案：**

**测试场景：** 16线程，每线程10000次读写

| 实现类 | 耗时 | 并发度 | 锁机制 |
|--------|------|--------|--------|
| **Hashtable** | ~3000ms | 1 | 全表锁（synchronized方法） |
| **Collections.synchronizedMap** | ~2800ms | 1 | 包装器锁 |
| **ConcurrentHashMap (JDK7)** | ~500ms | 16 | 分段锁 |
| **ConcurrentHashMap (JDK8)** | ~200ms | 无上限 | 桶级 CAS + synchronized |

**性能差距原因：**

Hashtable 的锁机制：
```java
public synchronized V put(K key, V value) {  // 全表锁！
    // ...
}

public synchronized V get(Object key) {  // 全表锁！
    // ...
}
```
- 所有读写操作竞争同一把锁
- 无法并发执行

ConcurrentHashMap：
- 读操作：无锁，直接读 volatile
- 写操作：只锁单个桶
- 不同桶的操作完全并发

---

### 8. 为什么 ConcurrentHashMap 读操作不需要加锁？

**答案：**

**原因分析：**

1. **volatile 保证可见性**
```java
static class Node<K,V> {
    final int hash;
    final K key;
    volatile V val;          // volatile！
    volatile Node<K,V> next; // volatile！
}
```

2. **Happens-Before 规则**
   - 对 volatile 的写 happens-before 于后续的读
   - 写操作完成后，读操作一定能看到最新值

3. **CAS 保证写入的原子性**
   - 写操作使用 CAS，不会出现半写状态
   - 读操作要么读到旧值，要么读到新值

4. **链表/红黑树结构**
   - 插入新节点是创建新 Node，不是修改现有 Node
   - 遍历不会受并发插入影响

**可能的例外情况：**
- size()：可能不精确（弱一致性）
- 遍历：可能读到过期数据
- clear()：非原子操作

---

## 线程安全实现

### 9. ConcurrentHashMap 如何保证线程安全？

**答案：**

**JDK8 线程安全策略：**

| 操作 | 线程安全机制 |
|------|-------------|
| **get** | 无锁，volatile 保证可见性 |
| **空桶 put** | CAS 原子插入 |
| **非空桶 put** | synchronized 锁头节点 |
| **扩容** | 多线程协作 + 桶级锁 |
| **size** | CounterCell CAS 计数 |
| **compute/merge** | synchronized 锁头节点 |

**原子操作方法：**
```java
// 原子条件插入
V putIfAbsent(K key, V value);

// CAS替换
boolean replace(K key, V oldValue, V newValue);

// 原子计算
V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction);

// 原子合并
V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction);
```

---

### 10. 使用 ConcurrentHashMap 时需要注意什么？

**答案：**

**注意事项：**

1. **size() 不是精确值**
   - 返回近似值（弱一致性）
   - 遍历期间可能变化

2. **遍历时可能看到过期数据**
   - 迭代器是弱一致性的
   - 不会抛出 ConcurrentModificationException

3. **复合操作需要原子方法**
   ```java
   // 错误：非原子操作
   if (!map.containsKey("key")) {
       map.put("key", value);  // 可能有并发问题
   }
   
   // 正确：原子操作
   map.putIfAbsent("key", value);
   ```

4. **compute 方法可能阻塞其他操作**
   - 锁住整个桶
   - 计算逻辑应尽量简单

5. **不要使用 null 键或 null 值**
   - ConcurrentHashMap 不允许 null
   - 会抛出 NullPointerException

6. **扩容时性能可能下降**
   - 协助扩容会消耗 CPU
   - 大容量时应预估初始容量

---

## 面试技巧

### 高频面试题 TOP 10

1. **ConcurrentHashMap 在 JDK7 和 JDK8 中分别是怎么实现的？**
2. **为什么 JDK8 放弃分段锁，改用 CAS + synchronized？**
3. **ConcurrentHashMap 的 get() 方法为什么不需要加锁？**
4. **size() 方法是怎么实现的？为什么不是精确值？**
5. **ConcurrentHashMap 是如何扩容的？支持多线程协助吗？**
6. **ConcurrentHashMap 和 Hashtable 的区别是什么？**
7. **JDK8 的 put() 方法执行流程是什么？**
8. **ConcurrentHashMap 的并发度是多少？**
9. **JDK8 的 CounterCell 是什么？解决了什么问题？**
10. **ConcurrentHashMap 是否支持 null 键值？为什么？**

### 答题模板

**问：JDK7 和 JDK8 中 ConcurrentHashMap 的实现有什么区别？**

**答：**

> JDK7 使用**分段锁（Segment）**实现，将数据分成多个 Segment，每个 Segment 是独立的 ReentrantLock。默认创建 16 个 Segment，最多支持 16 个线程并发写。读操作无需锁，使用 volatile 保证可见性。
>
> JDK8 放弃了分段锁，改用 **CAS + synchronized**。数据结构变为 Node 数组 + 链表/红黑树。写操作分为两种情况：空桶使用 CAS 直接插入，非空桶使用 synchronized 锁住链表头节点。这种实现锁粒度更细（桶级别），并发度更高（随扩容增加），且读操作完全无锁。
>
> JDK8 还做了其他优化：使用 CounterCell 分散 size 计算热点、支持多线程协助扩容、引入红黑树优化长链查询。这些改进使得 JDK8 的 ConcurrentHashMap 在高并发场景下性能更优。

---

*点击查看相关代码示例：[ConcurrentHashMapDemo.java](../code/ConcurrentHashMapDemo.java)*

*本文档持续更新，建议收藏备用*