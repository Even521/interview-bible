# CopyOnWriteArrayList 面试题

## 目录
1. [什么是 CopyOnWriteArrayList](#什么是-copyonwritearraylist)
2. [核心原理](#核心原理)
3. [适用场景](#适用场景)
4. [优缺点分析](#优缺点分析)
5. [与其他线程安全List对比](#与其他线程安全list对比)
6. [代码示例](../code/CopyOnWriteArrayListDemo.java)

---

## 什么是 CopyOnWriteArrayList

### 1. CopyOnWriteArrayList 是什么？

**答案：**

CopyOnWriteArrayList 是 `java.util.concurrent` 包提供的一个**线程安全的 List 实现**，采用**写时复制（Copy-On-Write）**策略实现读写分离。

**类定义：**
```java
public class CopyOnWriteArrayList<E> 
    implements List<E>, RandomAccess, Cloneable, Serializable {
    
    // volatile 修饰的数组，确保可见性
    private transient volatile Object[] array;
    
    // 写操作使用的锁
    private transient final ReentrantLock lock = new ReentrantLock();
}
```

**核心思想：** 读操作无锁，写操作加锁并复制新数组。

---

## 核心原理

### 2. CopyOnWriteArrayList 的实现原理是什么？

**答案：**

**读操作流程：**
```java
public E get(int index) {
    return get(getArray(), index); // 无锁，直接读
}

private E get(Object[] a, int index) {
    return (E) a[index]; // volatile 数组保证可见性
}
```

**写操作流程：**
```java
public boolean add(E e) {
    final ReentrantLock lock = this.lock;
    lock.lock(); // 加锁
    try {
        Object[] elements = getArray();
        int len = elements.length;
        Object[] newElements = Arrays.copyOf(elements, len + 1); // 复制
        newElements[len] = e; // 写入新数组
        setArray(newElements); // 原子替换
        return true;
    } finally {
        lock.unlock();
    }
}
```

**写时复制过程图示：**
```
写操作前：
Thread A(读) ──→ [A, B, C] ←── Thread B(读)
Thread C(写) 加锁并复制...

写操作中：
Thread A(读) ──→ [A, B, C] (旧快照，仍可读)
Thread C(写) ──→ [A, B, C, D] (新数组)

写操作后：
Thread A(读) ──→ [A, B, C] (旧数组，逐渐被GC)
Thread B(读) ──→ [A, B, C, D] (新数组)
```

### 3. 为什么读操作不需要加锁？

**答案：**

**原因分析：**

1. **volatile 保证可见性**
   - `array` 字段被 `volatile` 修饰
   - 写操作 `setArray(newElements)` 会触发 volatile 写屏障
   - 读操作 `getArray()` 会触发 volatile 读屏障
   - 保证新数组对所有线程立即可见

2. **读操作不修改数据**
   - 只读取数组引用和元素
   - 不涉及并发修改，无需同步

3. **数组复制机制**
   - 写操作创建新数组，旧数组保持不变
   - 读操作读取的是旧数组的不可变快照
   
---

## 适用场景

### 4. CopyOnWriteArrayList 适用于什么场景？

**答案：**

**核心条件：读多写少（读:写 >= 9:1）**

**典型应用场景：**

| 场景 | 特点 | 示例 |
|------|------|------|
| **事件监听器** | 注册一次，频繁触发 | GUI事件、业务事件监听 |
| **配置项列表** | 启动加载，运行读取 | IP白名单、开关配置 |
| **路由表** | 规则稳定，请求频繁 | API网关路由、灰度规则 |
| **热键缓存** | 批量更新，高频查询 | 缓存热点Key列表 |
| **观察者列表** | 订阅少，通知多 | 消息订阅者列表 |

**事件监听器示例：**
```java
class EventManager {
    private CopyOnWriteArrayList<EventListener> listeners = 
        new CopyOnWriteArrayList<>();
    
    public void addListener(EventListener listener) {
        listeners.add(listener);
    }
    
    public void fireEvent(String event) {
        // 读操作频繁，遍历所有监听器
        for (EventListener listener : listeners) {
            listener.onEvent(event);
        }
        // 遍历过程中可以安全添加/删除监听器
    }
}
```

### 5. 哪些场景不适合使用 CopyOnWriteArrayList？

**答案：**

| 场景 | 原因 | 替代方案 |
|------|------|----------|
| **写操作频繁** | 每次写都复制整个数组，O(n) | `Collections.synchronizedList` |
| **数据量大** | 复制大数组开销大 | `ConcurrentHashMap`（按key分片） |
| **需要实时一致性** | 读可能看到旧数据 | `Collections.synchronizedList` |
| **内存敏感** | 写操作存在两个数组 | 普通 `ArrayList` + 手动同步 |

---

## 优缺点分析

### 6. CopyOnWriteArrayList 的优缺点是什么？

**答案：**

**优点：**

| 优点 | 说明 |
|------|------|
| **读性能高** | 读操作无需加锁，并发性能极好 |
| **线程安全** | 无需外部同步，使用简单 |
| **迭代器安全** | 不会抛出 ConcurrentModificationException，可边遍历边修改 |
| **弱一致性** | 适合对实时性要求不高的读场景 |

**缺点：**

| 缺点 | 说明 |
|------|------|
| **写性能差** | 每次写都复制整个数组，O(n) |
| **内存开销大** | 写操作期间内存中存在两个数组 |
| **数据一致性弱** | 读操作可能读到旧数据 |
| **仅适合小数据量** | 数据量大时复制开销巨大 |

---

## 与其他线程安全List对比

### 7. CopyOnWriteArrayList 和其他线程安全 List 有何区别？

**答案：**

**对比表格：**

| 特性 | CopyOnWriteArrayList | Collections.synchronizedList | Vector | ConcurrentLinkedQueue |
|------|---------------------|----------------------------|--------|---------------------|
| **实现原理** | 写时复制 | synchronized | synchronized | CAS无锁 |
| **读锁** | 无 | synchronized | synchronized | 无 |
| **写锁** | 有（ReentrantLock） | synchronized | synchronized | CAS |
| **读多写少** | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐ | ⭐⭐⭐ |
| **读写均衡** | ⭐ | ⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐ |
| **内存占用** | ⭐⭐（高） | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **迭代器安全** | ⭐⭐⭐⭐⭐ | ⭐⭐（需手动同步） | ⭐⭐ | ⭐⭐⭐⭐ |
| **数据一致性** | 弱 | 强 | 强 | 弱 |

**选择建议：**

```
读多写少 + 数据量小 → CopyOnWriteArrayList
读写均衡 + 强一致性 → Collections.synchronizedList
高并发 + 队列场景   → ConcurrentLinkedQueue
避免使用           → Vector（已过时）
```

---

## 常见面试题

### 8. CopyOnWriteArrayList 的迭代器有什么特点？

**答案：**

**弱一致性迭代器（Snapshot Iterator）：**

```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
list.add("A");
list.add("B");

Iterator<String> itr = list.iterator(); // 获取快照
list.add("C"); // 修改原列表

while (itr.hasNext()) {
    System.out.println(itr.next()); // 输出 A, B（不包含C）
}

// 新迭代器能看到C
Iterator<String> newItr = list.iterator();
// 输出 A, B, C
```

**特点：**
- 迭代器创建时获取数组快照
- 遍历的是快照，不受后续修改影响
- 不会抛出 `ConcurrentModificationException`
- 可边遍历边修改原列表

### 9. CopyOnWriteArrayList 为什么用 volatile 修饰数组？

**答案：**

**volatile 的作用：**

1. **保证可见性**
   - 写线程 `setArray(newArray)` 后，新数组立即可见
   - 读线程 `getArray()` 获取的是最新数组

2. **保证有序性**
   - 防止指令重排序
   - 确保数组引用替换发生在元素写入之后

3. **实现无锁读**
   - 通过 volatile + 数组复制，实现读写分离
   - 读线程无需加锁，直接读取 volatile 数组

### 10. 使用 CopyOnWriteArrayList 要注意什么？

**答案：**

**注意事项：**

1. **评估读写比例**
   - 确保读:写 >= 9:1
   - 写多场景避免使用

2. **评估数据量**
   - 数据量大时复制开销大
   - 建议元素数 < 1000

3. **接受弱一致性**
   - 读可能看到旧数据
   - 不适合金融交易等强一致性场景

4. **写操作批量进行**
   - 减少复制次数
   - 可使用 `addAllAbsent` 批量添加

5. **迭代器特性**
   - 遍历的是快照
   - 不支持 `remove()` 操作

6. **内存占用**
   - 写操作期间存在两个数组
   - 注意 GC 压力

---

## 面试技巧

### 答题模板

**问：CopyOnWriteArrayList 是什么？适用于什么场景？**

**答：**

> CopyOnWriteArrayList 是 `java.util.concurrent` 包提供的线程安全 List 实现，采用**写时复制**的策略实现线程安全。
>
> **核心原理**是读操作不加锁，直接读取底层数组；写操作加锁，复制一个新数组，在新数组上修改后原子替换原数组引用。这样可以实现读写分离，读操作并发性能极高。
>
> 它适用于**读多写少**的场景，典型应用包括事件监听器列表（注册少触发多）、配置项列表（启动后很少修改，频繁读取）、路由表、白名单等。
>
> 需要注意的是，每次写操作都要复制整个数组，所以**写性能差**，只适合数据量小的场景。如果写操作频繁或数据量大，应该使用 `Collections.synchronizedList` 或其他并发集合。此外，读操作是弱一致性，可能读到旧数据，不适合需要实时一致性的金融交易等场景。

---

*点击查看相关代码示例：[CopyOnWriteArrayListDemo.java](../code/CopyOnWriteArrayListDemo.java)*

*本文档持续更新，建议收藏备用*