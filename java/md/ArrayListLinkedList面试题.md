# ArrayList 和 LinkedList 面试题

## 目录
1. [底层数据结构对比](#底层数据结构对比)
2. [扩容机制](#扩容机制)
3. [性能对比](#性能对比)
4. [内存占用对比](#内存占用对比)
5. [使用场景](#使用场景)
6. [代码示例](../code/ArrayListVsLinkedListDemo.java)

---

## 底层数据结构对比

### 1. ArrayList 和 LinkedList 的底层数据结构有什么区别？

**答案：**

| 特性 | ArrayList | LinkedList |
|------|-----------|------------|
| **底层结构** | Object[] 动态数组 | Node<E> 双向链表 |
| **存储方式** | 内存连续，支持随机访问 | 内存分散，只能顺序访问 |
| **访问方式** | 通过索引 O(1) | 需要遍历 O(n) |
| **节点结构** | 单一数组元素 | item + next + prev |

**ArrayList 结构：**
```java
public class ArrayList<E> {
    transient Object[] elementData; // 存储元素的数组
    private int size;               // 实际元素数量
}
// 内存布局：[0][1][2][3][...][n] 连续存储
```

**LinkedList 结构：**
```java
public class LinkedList<E> {
    transient Node<E> first;  // 头节点
    transient Node<E> last;   // 尾节点
    
    private static class Node<E> {
        E item;           // 数据
        Node<E> next;     // 后继指针
        Node<E> prev;     // 前驱指针
    }
}
// 内存布局：[数据|next|prev] <-> [数据|next|prev] <-> ...
```

---

## 扩容机制

### 2. ArrayList 的扩容机制是怎样的？

**答案：**

**扩容时机：** 当添加元素时，size == elementData.length（数组已满）

**扩容策略：**
- 新容量 = 旧容量 + (旧容量 >> 1) = 旧容量的 **1.5倍**
- 使用 `Arrays.copyOf()` 创建新数组并复制数据

**扩容源码：**
```java
private void grow(int minCapacity) {
    int oldCapacity = elementData.length;
    // 新容量 = 旧容量的1.5倍
    int newCapacity = oldCapacity + (oldCapacity >> 1);
    if (newCapacity - minCapacity < 0)
        newCapacity = minCapacity;
    // 复制数据到新数组
    elementData = Arrays.copyOf(elementData, newCapacity);
}
```

**扩容过程：**
```
初始：elementData = [null, null, null, null, null, null, null, null, null, null]
      size = 0, capacity = 10

添加11个元素后触发扩容：
旧容量：10
新容量：10 + (10 >> 1) = 10 + 5 = 15
创建新数组：[..., ..., ..., ..., ..., ..., ..., ..., ..., ..., ..., ..., ..., ..., ...]
复制数据：将前10个元素复制到新数组
```

**优化建议：**
```java
// 预估容量，一次性分配，避免多次扩容
List<String> list = new ArrayList<>(10000);
```

---

## 性能对比

### 3. ArrayList 和 LinkedList 的性能对比？

**答案：**

| 操作 | ArrayList | LinkedList | 说明 |
|------|-----------|------------|------|
| **get(index)** | O(1) ✅ | O(n) ❌ | ArrayList直接定位，LinkedList需要遍历 |
| **add(E)** | O(1)* | O(1) | ArrayList尾部添加，可能需要扩容 |
| **add(0, E)** | O(n) ❌ | O(1) ✅ | ArrayList需要移动所有元素 |
| **add(middle, E)** | O(n) | O(n) | 都需要遍历到指定位置 |
| **remove(0)** | O(n) ❌ | O(1) ✅ | ArrayList需要移动所有元素 |
| **remove(end)** | O(1) | O(1) | 都很快 |
| **contains(E)** | O(n) | O(n) | 都需要线性查找 |
| **内存占用** | 较少 | 较多 | LinkedList每个节点有额外指针开销 |

**性能测试：**
```java
// 随机访问 100000 次
ArrayList:   1-2 ms (O(1))
LinkedList:  5000+ ms (O(n)) - 慢1000倍！

// 头部插入 10000 次
ArrayList:   100+ ms (需要移动元素)
LinkedList:  1-2 ms (修改指针即可)
```

---

## 内存占用对比

### 4. ArrayList 和 LinkedList 的内存占用有什么区别？

**答案：**

**ArrayList 内存结构：**
- Object[] elementData（数组）
- 数组可能预留空间（capacity >= size）
- 元素连续存储

```
内存 = 对象头 + size字段 + elementData引用 + 数组对象
     = 16 + 4 + 4 + (capacity * 引用大小)
```

**LinkedList 内存结构：**
- 每个节点包含：item + next + prev（3个引用）
- 节点分散在堆内存各处

```
内存 = 头尾指针 + size字段 + n个节点
     = 对象头 + first引用 + last引用 + size字段
       + n * (Node对象头 + item引用 + next引用 + prev引用 + 对齐填充)
```

**对比结论：**
- ArrayList 更节省内存（无节点指针开销）
- LinkedList 额外开销大（每个节点3个引用+对象头）
- 但 ArrayList 可能浪费空间（预留未使用的capacity）

---

## 使用场景

### 5. 什么时候用 ArrayList，什么时候用 LinkedList？

**答案：**

**选择 ArrayList（默认推荐）：**
- ✅ 随机访问多（通过索引获取元素）
- ✅ 遍历操作多
- ✅ 尾部插入删除多
- ✅ 内存敏感
- ✅ 元素数量相对稳定

**选择 LinkedList：**
- ✅ 头部/中部插入删除频繁
- ✅ 需要实现栈或队列（Deque接口）
- ✅ 实现 LRU Cache 等算法
- ✅ 元素数量变化大（无需扩容）

**LinkedList 的 Deque 特性：**
```java
LinkedList<String> deque = new LinkedList<>();

// 栈操作（后进先出）
deque.push("First");
deque.push("Second");
String top = deque.pop(); // "Second"

// 队列操作（先进先出）
deque.offer("A");
deque.offer("B");
String head = deque.poll(); // "A"
```

---

### 6. 为什么遍历 LinkedList 不要用 for 循环？

**答案：**

**错误的遍历方式：**
```java
// 每次 get(i) 都需要从头遍历，O(n²) 复杂度！
for (int i = 0; i < list.size(); i++) {
    System.out.println(list.get(i)); // O(n) per call
}
// 总复杂度：O(n²)
```

**正确的遍历方式：**
```java
// 方式1：使用迭代器（推荐）- O(n)
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    System.out.println(it.next()); // O(1)
}

// 方式2：使用 for-each（编译成迭代器）- O(n)
for (String s : list) {
    System.out.println(s);
}
```

**原因分析：**
- `LinkedList.get(index)` 需要从头或尾遍历到指定位置，O(n)
- `Iterator.next()` 只需要移动指针，O(1)
- for-each 循环会被编译成迭代器遍历

---

## 常见误区

### 7. ArrayList 和 LinkedList 都是线程安全的吗？

**答案：**

**都不是线程安全的！**

**线程不安全问题：**
```java
List<String> list = new ArrayList<>();
// 多线程并发添加可能丢数据
```

**线程安全解决方案：**
```java
// 方式1：使用 Collections.synchronizedList
List<String> syncList = Collections.synchronizedList(new ArrayList<>());

// 方式2：使用 CopyOnWriteArrayList（读多写少）
List<String> cowList = new CopyOnWriteArrayList<>();

// 方式3：使用 Vector（不推荐，遗留类）
List<String> vector = new Vector<>();
```

---

### 8. 什么情况下 LinkedList 的插入比 ArrayList 慢？

**答案：**

**LinkedList 的 add(index, E) 可能比 ArrayList 慢！**

**原因：**
- LinkedList.add(index, E) 需要先遍历到指定位置 O(n)
- ArrayList.add(index, E) 虽然需要移动元素，但内存连续，移动很快

**实验对比：**
```java
// 在大约 size/2 位置插入
ArrayList:   移动 n/2 个元素（内存连续，很快）
LinkedList:  遍历 n/2 个节点找到位置（分散内存，较慢）

// 结果：ArrayList 可能反而更快！
```

**LinkedList 真正快的场景：**
- 头部插入：`add(0, E)` 或 `addFirst(E)`
- 尾部插入：`add(E)` 或 `addLast(E)`

---

## 代码示例

### 9. 如何正确选择和使用 ArrayList/LinkedList？

**答案：**

```java
// 默认使用 ArrayList（大部分场景性能更好）
List<String> list = new ArrayList<>();

// 需要频繁头部操作时，考虑 LinkedList
Deque<String> deque = new LinkedList<>();

// 预估容量，避免扩容
List<String> largeList = new ArrayList<>(10000);

// 遍历 LinkedList 必须用迭代器
LinkedList<String> linkedList = new LinkedList<>();
for (String s : linkedList) { // 正确！编译成迭代器
    // ...
}

// 绝对不要用 get(index) 遍历 LinkedList
// for (int i = 0; i < linkedList.size(); i++) // ❌ 错误！O(n²)
```

---

## 面试技巧

### 高频面试题 TOP 10

1. **ArrayList 和 LinkedList 的底层结构有什么区别？**
2. **ArrayList 的扩容机制是怎样的？**
3. **为什么遍历 LinkedList 不要用 for 循环？**
4. **ArrayList 和 LinkedList 分别适用于什么场景？**
5. **LinkedList 的内存占用为什么比 ArrayList 多？**
6. **ArrayList 1.5倍扩容是怎么计算的？**
7. **LinkedList 继承了哪些接口？Deque 有什么用？**
8. **ArrayList 和 LinkedList 是线程安全的吗？**
9. **LinkedList.get(index) 的时间复杂度是多少？为什么？**
10. **说一说 ArrayList 和 LinkedList 的优缺点？**

### 答题模板

**问：ArrayList 和 LinkedList 有什么区别，分别适用于什么场景？**

**答：**

> ArrayList 和 LinkedList 是 Java 中最常用的两种 List 实现，它们有本质的区别。
> 
> **底层结构上**，ArrayList 基于动态数组，使用 Object[] 存储元素，内存连续，支持随机访问；LinkedList 基于双向链表，每个节点包含数据和前后指针，内存分散，只能顺序访问。
> 
> **性能方面**，ArrayList 的 get(index) 是 O(1)，很快；但插入删除需要移动元素，尤其是头部操作是 O(n)。LinkedList 的 get(index) 是 O(n) 因为需要遍历；但插入删除只需要修改指针，头部操作是 O(1)。
> 
> **内存占用上**，ArrayList 更节省内存，只有数组的开销，可能有些预留空间；LinkedList 每个节点有额外的前后指针开销，内存占用更大。
> 
> **适用场景**：默认推荐使用 ArrayList，因为大部分场景都是查询多；只有当需要频繁在头部或中部插入删除，或者需要实现栈和队列时，才使用 LinkedList。遍历 LinkedList 一定要用迭代器，绝对不要用 for 循环配合 get(index)，那样会变成 O(n²) 的复杂度。

---

*点击查看相关代码示例：[ArrayListVsLinkedListDemo.java](../code/ArrayListVsLinkedListDemo.java)*

*本文档持续更新，建议收藏备用*