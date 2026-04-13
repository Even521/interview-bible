# equals 与 hashCode 面试题

## 目录
1. [== vs equals](#-vs-equals)
2. [equals 契约](#equals-契约)
3. [hashCode 契约](#hashcode-契约)
4. [为什么重写 equals 必须重写 hashCode](#为什么重写-equals-必须重写-hashcode)
5. [常见错误](#常见错误)
6. [代码示例](../code/EqualsHashCodeDemo.java)

---

## == vs equals

### 1. == 和 equals 的区别是什么？

**答案：**

| 特性 | == | equals |
|------|-----|--------|
| **基本类型** | 比较**值** | 不可用 |
| **引用类型** | 比较**内存地址** | 默认比较地址，重写后比较内容 |
| **String** | 比较地址（常量池有优化） | 比较内容（已重写） |
| **Integer** | -128~127 缓存内相同 | 比较值（已重写） |

**示例：**

```java
// 1. 基本类型
int a = 10, b = 10;
a == b; // true，比较值

// 2. 引用类型
Person p1 = new Person("Alice");
Person p2 = new Person("Alice");
p1 == p2;     // false，地址不同
p1.equals(p2); // false（未重写时）

// 3. String 常量池
String s1 = "hello";
String s2 = "hello";
String s3 = new String("hello");
s1 == s2; // true（常量池复用）
s1 == s3; // false（new 创建新对象）
s1.equals(s3); // true（内容相同）

// 4. Integer 缓存陷阱
Integer i1 = 127, i2 = 127;
Integer i3 = 128, i4 = 128;
i1 == i2; // true（-128~127 缓存）
i3 == i4; // false（超出缓存范围）
```

---

## equals 契约

### 2. equals 的四大契约是什么？

**答案：**

**Object.equals() 方法规范（必须遵循）：**

| 特性 | 说明 |
|------|------|
| **自反性** | `x.equals(x)` 必须返回 `true` |
| **对称性** | `x.equals(y) == y.equals(x)` |
| **传递性** | 若 `x.equals(y)` 且 `y.equals(z)`，则 `x.equals(z)` |
| **一致性** | 多次调用 `x.equals(y)` 应返回相同结果 |
| **非空性** | `x.equals(null)` 必须返回 `false` |

**正确实现步骤：**

```java
@Override
public boolean equals(Object obj) {
    // 1. 自反性：同一个对象
    if (this == obj) return true;
    
    // 2. 非空性：null 检查
    if (obj == null) return false;
    
    // 3. 类型检查
    // 方法1：getClass() - 严格类型匹配（推荐非 final 类）
    if (getClass() != obj.getClass()) return false;
    
    // 方法2：instanceof - 允许子类（可能破坏对称性）
    // if (!(obj instanceof Person)) return false;
    
    // 4. 向下转型
    Person other = (Person) obj;
    
    // 5. 比较字段
    // 基本类型用 ==
    // 引用类型用 Objects.equals（处理 null）
    return age == other.age 
        && Objects.equals(name, other.name);
}
```

---

## hashCode 契约

### 3. hashCode 的两大契约是什么？

**答案：**

**hashCode 方法规范：**

| 特性 | 说明 |
|------|------|
| **一致性** | 多次调用 `hashCode()` 返回相同值（对象未修改时） |
| **相等性** | `x.equals(y)` ⇒ `x.hashCode() == y.hashCode()` |

**重要理解：**
- 逆命题**不成立**：hashCode 相同，equals 不一定相同（哈希碰撞）
- 不相等对象**可以**有相同 hashCode

**哈希碰撞示例：**

```java
"Aa".hashCode(); // 2112
"BB".hashCode(); // 2112

"Aa".equals("BB"); // false，hashCode 相同但不相等
```

---

## 为什么重写 equals 必须重写 hashCode

### 4. 为什么重写 equals 必须重写 hashCode？

**答案：**

**核心原因：HashMap/HashSet/HashTable 等基于哈希的集合依赖 hashCode 定位元素**

**问题演示：**

```java
public class Person {
    String name;
    int age;
    
    @Override
    public boolean equals(Object o) {
        // 比较 name 和 age
    }
    // 没有重写 hashCode！
}

// 测试
Person p1 = new Person("Alice", 25);
Person p2 = new Person("Alice", 25);

Map<Person, String> map = new HashMap<>();
map.put(p1, "Data 1");
map.put(p2, "Data 2");

// 结果：map.size() = 2（预期应该是 1）
// 原因：p1 和 p2 的 hashCode 不同（默认基于地址），放入了不同桶
```

**HashMap 查找流程：**

```java
// HashMap.get(key) 流程：
public V get(Object key) {
    // 1. 根据 hashCode 计算桶位置
    int hash = hash(key.hashCode());
    int index = (n - 1) & hash;
    
    // 2. 遍历桶中链表，用 equals 比较
    for (Node<K,V> e = table[index]; e != null; e = e.next) {
        if (e.hash == hash && e.key.equals(key))
            return e.value;
    }
    return null;
}
```

**关键点：**
1. 先根据 `hashCode` 找到桶位置
2. 再用 `equals` 比较链表元素
3. 如果只重写 equals，hashCode 不同 → 找错桶 → 即使 equals 相同也找不到

### 5. hashCode 的正确实现方法？

**答案：**

**方式1：IDE 自动生成（推荐）**

```java
@Override
public int hashCode() {
    return Objects.hash(name, age);
}
```

**方式2：手动计算（性能敏感）**

```java
@Override
public int hashCode() {
    int result = 17;           // 初始非零值
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + age;
    return result;
}
```

**选择 31 的原因：**
1. 奇素数，减少哈希冲突
2. `31 * i = (i << 5) - i`，JVM 可优化为位移

**必须包含的字段：**
- 参与 equals 比较的所有字段，都必须参与 hashCode 计算

---

## 常见错误

### 6. equals/hashCode 常见错误有哪些？

**答案：**

| 错误 | 说明 | 解决 |
|------|------|------|
| **只重写 equals** | 导致 HashMap 行为异常 | 同时重写 hashCode |
| **重写时参数错误** | `equals(Person o)` 是重载不是重写 | 参数必须是 `Object` |
| **使用可变字段** | 对象放入 HashSet 后修改字段，找不到 | 使用不可变对象或不可变字段 |
| **浮点数用 ==** | `double == double` 不精确 | `Double.compare(d1, d2)` |
| **包含集合/数组** | 引用相等 vs 内容相等 | 使用 `Objects.deepEquals` |

**可变字段陷阱：**

```java
Set<Person> set = new HashSet<>();
Person p = new Person("Alice", 25);
set.add(p);
set.contains(p); // true

p.name = "Bob"; // 修改参与 hashCode 的字段
set.contains(p); // false！hashCode 变了，找错桶

// 原因：修改后 hashCode 变了，去错误的桶查找
```

### 7. getClass() vs instanceof 用哪个？

**答案：**

| 方法 | 特点 | 使用场景 |
|------|------|----------|
| **getClass()** | 严格类型匹配 | 非 final 类（防止子类破坏对称性） |
| **instanceof** | 允许子类 | final 类或需要多态 |

**子类破坏对称性问题：**

```java
class Person {
    String name;
    
    @Override
    public boolean equals(Object obj) {
        // 使用 instanceof
        if (!(obj instanceof Person)) return false;
        Person other = (Person) obj;
        return Objects.equals(name, other.name);
    }
}

class Student extends Person {
    int studentId;
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Student)) return false;
        // ...
    }
}

// 问题：
Person p = new Person("Alice");
Student s = new Student("Alice", 123);
p.equals(s); // true（Person 的视角）
s.equals(p); // false（Student 的视角）
// 破坏了对称性！
```

**解决方案：**
```java
// 非 final 类使用 getClass()
@Override
public boolean equals(Object obj) {
    if (obj == null || getClass() != obj.getClass()) 
        return false;
    // ...
}
```

---

## 最佳实践

### 8. equals/hashCode 最佳实践？

**答案：**

**1. 使用 IDE 自动生成**
```java
// IntelliJ IDEA: Alt + Insert → equals() and hashCode()
// Eclipse: Alt + Shift + S → Generate hashCode() and equals()
```

**2. 使用 `Objects` 工具类（JDK 7+）**
```java
@Override
public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    Person that = (Person) obj;
    return age == that.age && 
           Objects.equals(name, that.name); // 处理 null
}

@Override
public int hashCode() {
    return Objects.hash(name, age); // 简洁写法
}
```

**3. 用于 HashMap key 的对象应保持不可变**
```java
public final class ImmutablePerson {
    private final String name;  // final 修饰
    private final int age;
    
    // 创建后不可修改，hashCode 永远不变
}
```

**4. 单元测试验证契约**
```java
@Test
public void testEqualsContract() {
    Person x = new Person("Alice", 25);
    Person y = new Person("Alice", 25);
    Person z = new Person("Alice", 25);
    
    // 自反性
    assertTrue(x.equals(x));
    
    // 对称性
    assertTrue(x.equals(y) == y.equals(x));
    
    // 传递性
    assertTrue(x.equals(y) && y.equals(z) && x.equals(z));
    
    // 一致性
    assertTrue(x.equals(y) == x.equals(y));
    
    // 非空性
    assertFalse(x.equals(null));
    
    // hashCode 关系
    assertTrue(x.hashCode() == y.hashCode());
}
```

### 9. 为什么 String 的 hashCode 会有缓存？

**答案：**

String 是**不可变对象**，hashCode 可以缓存避免重复计算：

```java
public final class String {
    private int hash; // 缓存 hashCode，默认 0
    
    public int hashCode() {
        int h = hash;               // 先读取缓存
        if (h == 0 && value.length > 0) {
            // 计算 hashCode
            char val[] = value;
            for (int i = 0; i < value.length; i++) {
                h = 31 * h + val[i];
            }
            hash = h;               // 存入缓存
        }
        return h;
    }
}
```

**优点：**
1. String 常用作 HashMap key，频繁计算 hashCode
2. 不可变性保证 hashCode 一旦计算就不会改变
3. 节省 CPU，提高性能

---

## 面试技巧

### 高频面试题 TOP 10

1. **== 和 equals 的区别？**
2. **为什么重写 equals 必须重写 hashCode？**
3. **equals 的四大契约是什么？**
4. **hashCode 的两大契约是什么？**
5. **两个对象 hashCode 相同，equals 一定相同吗？**
6. **getClass() 和 instanceof 在 equals 中用哪个？为什么？**
7. **可变对象作为 HashMap key 有什么问题？**
8. **String 的 hashCode 为什么可以缓存？**
9. **Objects.equals 和 == 的区别？**
10. **如何实现一个线程安全的 equals 和 hashCode？**

### 答题模板

**问：为什么重写 equals 必须重写 hashCode？**

**答：**

> 重写 equals 必须同时重写 hashCode，是因为基于哈希的集合（HashMap、HashSet、HashTable）使用 hashCode 来定位元素存储的桶位置。
>
> HashMap 的查找流程是：先根据 key 的 hashCode 计算桶索引（index = hash & (n-1)），然后遍历该桶的链表，用 equals 比较找到具体元素。
>
> 如果只重写 equals 而不重写 hashCode，会导致相等的对象有不同的 hashCode，从而被放入不同的桶。那么在 HashMap.get() 时，根据 hashCode 会找错桶，即使 equals 相同也找不到对应元素，造成数据丢失。
>
> 例如两个内容相同的 Person 对象，如果只重写 equals，它们的 hashCode 默认基于内存地址，放入 HashMap 会被视为不同的 key。所以 hashCode 必须与 equals 保持一致：equals 相等的对象，hashCode 必须相同。

---

*点击查看相关代码示例：[EqualsHashCodeDemo.java](../code/EqualsHashCodeDemo.java)*

*本文档持续更新，建议收藏备用*