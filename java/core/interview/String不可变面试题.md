<file_path> interview-bible\java\md\String不可变面试题.md </file_path>
<edit_description> 创建 String 不可变性面试题文档 </edit_description>

# String 不可变性（Immutable）面试题

## 目录
1. [什么是不可变](#什么是不可变)
2. [为什么设计为不可变](#为什么设计为不可变)
3. [字符串常量池](#字符串常量池)
4. [JDK 9+ 优化](#JDK-9优化)
5. [常见误区](#常见误区)
6. [代码示例](../code/StringImmutableDemo.java)

---

## 什么是不可变

### 1. 为什么说 String 是不可变的？

**答案：**

String 不可变是指：**一旦创建，其内容无法被修改**。

**源码证据：**

```java
public final class String
    implements java.io.Serializable, Comparable<String>, CharSequence {
    
    // JDK 8: private final char[] value;
    // JDK 9+: private final byte[] value;  // 编码：LATIN1(0) / UTF16(1)
    private final byte[] value;
    
    // 计算后的 hashCode 缓存
    private int hash; 
}
```

**不可变的三个层面：**

| 层面 | 说明 |
|------|------|
| **类被 final 修饰** | 不能被继承，防止子类破坏不可变性 |
| **数组引用 final** | `value` 字段指向的数组地址不能改变 |
| **不提供修改方法** | 没有提供修改数组内容的 public 方法 |

**验证代码：**

```java
String str = "Hello";
str.concat(" World");  // 返回新对象，原字符串不变
str.toUpperCase();     // 返回新对象，原字符串不变

System.out.println(str); // 输出 "Hello"，从未改变！
```

---

## 为什么设计为不可变

### 2. String 为什么要设计为不可变的？有哪些好处？

**答案：**

**五大核心原因：**

| 原因 | 说明 | 好处 |
|------|------|------|
| **安全性** | 字符串常用作敏感数据（密码、路径、URL） | 防止被恶意修改，保证数据安全 |
| **线程安全** | 内容不可变 | 天然线程安全，无需同步 |
| **HashCode 缓存** | hashCode 计算一次后缓存 | 提高 HashMap 性能 |
| **字符串常量池** | 相同字符串共享引用 | 节省内存，快速比较 |
| **类加载安全** | 类名、包名用 String 存储 | 确保类加载器不会被欺骗 |

**详细说明：**

**1. 安全性（Security）**

```java
// 如果 String 可变，会发生什么灾难？
String password = "secret123";
UserLogin.login(password);

// 恶意代码如果修改了 password，会导致安全问题
// 不可变性确保密码无法被篡改

// String 作为网络连接参数
Socket socket = new Socket(hostname, port);
// 如果 hostname 可变，可能被劫持到恶意服务器
```

**2. 线程安全（Thread Safety）**

```java
// String 天然线程安全
String shared = "Shared";

// 多个线程同时读取，无需同步
Thread t1 = new Thread(() -> System.out.println(shared));
Thread t2 = new Thread(() -> System.out.println(shared));
// 不会有任何并发问题
```

**3. HashCode 缓存**

```java
// String.hashCode() 源码
public int hashCode() {
    int h = hash;      // 先读取缓存
    if (h == 0 && value.length > 0) {
        // 计算 hashCode
        for (int i = 0; i < value.length; i++) {
            h = 31 * h + value[i];
        }
        hash = h;      // 存入缓存
    }
    return h;
}
```

**优点：**
- 作为 HashMap key 时，频繁使用 hashCode，缓存避免重复计算
- 不可变性保证 hashCode 一旦计算就不会改变

**4. 字符串常量池（String Pool）**

```java
String s1 = "Java";  // 入常量池
String s2 = "Java";  // 从常量池复用

System.out.println(s1 == s2); // true，同一对象
```

**为什么需要不可变？**
- 如果 String 可变，常量池中的字符串被修改，会影响所有引用
- 例如：`s1.toUpperCase()` 修改后，s2 也会变成大写，产生 bug

**5. 类加载安全**

```java
// 类加载时使用 String 作为类名
Class.forName("java.lang.String");

// 如果 String 可变，类名被篡改，可能加载到恶意类
```

---

## 字符串常量池

### 3. 字符串常量池是什么？如何运作？

**答案：**

**常量池位置演进：**

| JDK 版本 | 位置 | 说明 |
|---------|------|------|
| JDK 6 | 永久代（PermGen） | 大小固定，容易出现 OOM |
| JDK 7+ | 堆内存（Heap） | 可 GC，节省 PermGen 空间 |
| JDK 8+ | 元空间（Metaspace）类的常量池在这里，String Pool 在堆 | 自动扩展 |

**创建方式对比：**

```java
// 方式1：字面量（推荐，自动入池）
String s1 = "Java";

// 方式2：new 创建（不入池，堆内存新对象）
String s2 = new String("Java");

// 方式3：intern() 入池
String s3 = new String("Java").intern();
```

**内存图示：**

```
堆内存
├── 字符串常量池
│   ├── "Java" ←──┐
│   └── "Python"  │
├── s1 ──────────┘（引用常量池）
├── s3 ──────────┘（intern 后引用常量池）
└── s2 ──→ "Java"（堆内存新对象，new 创建）
```

**intern() 方法：**

```java
String s1 = new String("Hello");  // 堆内存对象
String s2 = s1.intern();          // 入常量池，返回常量池引用

String s3 = "Hello";              // 字面量，引用常量池

System.out.println(s2 == s3);     // true
```

**使用建议：**
- 优先使用字面量创建：`String s = "abc";`
- 避免重复创建相同字符串：`new String("abc")` ×
- 大量重复字符串使用 intern() 节省内存（但要谨慎，可能占用大量堆内存）

---

### 4. String s = new String("abc") 创建了几个对象？

**答案：**

**创建 1 或 2 个对象：**

| 情况 | 对象数量 | 说明 |
|------|---------|------|
| 常量池已有 "abc" | 1 | 只在堆创建新 String 对象 |
| 常量池没有 "abc" | 2 | 常量池创建 "abc" + 堆创建 String 对象 |

**过程分析：**

```java
String s = new String("abc");
```

**执行步骤：**
1. **类加载时**："abc" 进入常量池（如果之前不存在）
2. **运行时**：`new String(...)` 在堆内存创建新的 String 对象
3. String 对象的 value 字段指向常量池中的 "abc"

**内存布局：**

```
常量池: "abc"

堆内存:
new String("abc")
├── value ──→ "abc"（常量池引用）
└── hash: 0

局部变量 s ──→ [堆 String 对象]

如果写成 String s = "abc"
直接指向常量池，不创建堆对象！
```

---

## JDK 9+ 优化

### 5. JDK 9 对 String 做了什么优化？

**答案：**

**核心优化：内部从 char[] 改为 byte[]**

```java
// JDK 8: char[]（2字节一个字符，无论 ASCII 还是中文）
private final char[] value;

// JDK 9+: byte[] + coder（节省内存）
private final byte[] value;    // 实际字节数据
private final byte coder;      // 编码标识：0=LATIN1, 1=UTF16
```

**优化原因：**

| 场景 | JDK 8 char[] | JDK 9+ byte[] |
|------|-------------|---------------|
| 纯英文 "Hello" | 10 字节（5 char × 2） | 5 字节（LATIN1，1字节/字符） |
| 包含中文 "你好" | 4 字节（2 char × 2） | 4 字节（UTF16，2字节/字符） |

**Compact Strings（紧凑字符串）：**
- **Latin1**（ISO-8859-1）：单字节编码，支持西欧字符
- **UTF-16**：双字节编码，支持所有 Unicode

**自动判断：**
```java
String s1 = "Hello";      // 纯 ASCII，使用 LATIN1（5 字节）
String s2 = "你好";         // 中文，使用 UTF-16（4 字节）
```

**字符串拼接优化：**

JDK 9+ 使用 `invokedynamic` + `StringConcatFactory`：
```java
// 编译前
String result = str1 + str2 + str3;

// JDK 8 编译后：StringBuilder
String result = new StringBuilder()
    .append(str1)
    .append(str2)
    .append(str3)
    .toString();

// JDK 9+ 编译后：invokedynamic
invokedynamic makeConcatWithConstants(String, String, String)String
```

**优点：**
- 运行时选择合适的拼接策略（StringBuilder、lambda 等）
- 未来可以进一步优化，无需修改字节码

---

## 常见误区

### 6. String 不可变，那它是怎么"改变"的？

**答案：**

**不是改变，是创建新对象！**

```java
String str = "Hello";

// 这行代码发生了什么？
str = str + " World";
```

**实际过程：**

```java
// 1. 创建 StringBuilder（JDK 8）或使用 invokedynamic（JDK 9+）
// 2. 在 StringBuilder 中拼接 "Hello" + " World"
// 3. 调用 toString() 创建新 String 对象
// 4. str 指向新对象，"Hello" 对象被丢弃（等待 GC）

// 实际上是：
String newStr = new StringBuilder("Hello")
    .append(" World")
    .toString();
str = newStr;  // 引用指向新对象
```

**图示：**

```
步骤1: str ──→ "Hello"

步骤2: str 引用改变
str ──→ StringBuilder("Hello World") ──→ new String("Hello World")
           "Hello" 对象等待 GC
           
步骤3: str ──→ "Hello World"
```

---

### 7. 频繁拼接字符串应该用什么？为什么不用 String？

**答案：**

**使用 StringBuilder（单线程）或 StringBuffer（多线程）**

| 类 | 线程安全 | 性能 | 使用场景 |
|----|---------|------|---------|
| **String** | ✓ 安全（不可变） | 慢（频繁创建对象） | 字符串不常改变 |
| **StringBuilder** | ✗ 不安全 | 快（无同步） | 单线程大量拼接 |
| **StringBuffer** | ✓ 安全（synchronized） | 较慢（有同步） | 多线程大量拼接 |

**性能对比：**

```java
// 极差：String + 在循环内
String result = "";
for (int i = 0; i < 10000; i++) {
    result += i;  // 每次创建新 String 和 StringBuilder，O(n²) 时间复杂度
}

// 很好：StringBuilder
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 10000; i++) {
    sb.append(i);  // O(n) 时间复杂度
}
String result = sb.toString();
```

**编译器优化：**

```java
// 编译器会自动优化为 StringBuilder
String s = "a" + "b" + "c";  // 编译时直接变成 "abc"

// 但循环内不会优化！
String s = "";
for (...) {
    s += i;  // 每次循环都创建新对象！
}
```

---

### 8. String.intern() 有什么风险？什么时候用？

**答案：**

**风险：内存泄漏和性能下降**

```java
// 危险：大量不同字符串入池
for (int i = 0; i < 1000000; i++) {
    String s = new String("Key" + i).intern();  // 一百万个不同字符串入池
    // 这些字符串不会被 GC，占用大量堆内存
}
```

**适用场景：**

1. **大量重复字符串**：如单词统计、DNA 序列分析
2. **字符串常量**：配置项、枚举值
3. **内存敏感但需要快速比较**：用空间换时间

**使用建议：**
- 确保字符串会重复出现，才使用 intern()
- 控制 intern() 的数量，避免内存溢出
- JDK 8+ 可以使用 `-XX:+UseStringDeduplication`（G1 GC）自动去重

---

## 面试技巧

### 高频面试题 TOP 10

1. **String 为什么是不可变的？设计原因是什么？**
2. **`new String("abc")` 创建了几个对象？**
3. **String、StringBuilder、StringBuffer 的区别？**
4. **String 的 hashCode 为什么要缓存？**
5. **字符串常量池在哪里？JDK 7 后为什么移到堆内存？**
6. **JDK 9 对 String 做了什么优化？**
7. **String.intern() 是什么？有什么风险？**
8. **String + 操作底层是怎么实现的？**
9. **如果 String 可变了，会有什么问题？**
10. **如何证明 String 是不可变的？**

### 答题模板

**问：String 为什么要设计为不可变的？**

**答：**

> String 设计为不可变主要有五大原因：
>
> **第一，安全性。** String 经常用作敏感数据的载体，如文件路径、网络地址、数据库密码等。如果 String 可变，这些内容可能被恶意代码篡改，造成安全漏洞。不可变性确保了数据一旦创建就不会被修改。
>
> **第二，线程安全。** 因为内容不可变，String 天然就是线程安全的，可以在多线程环境中自由共享，无需同步机制，节省同步开销。
>
> **第三，HashCode 缓存。** String 的 hashCode() 方法会计算一次后缓存结果。由于 String 不可变，缓存的 hashCode 永远不会过期，这对于频繁作为 HashMap key 的 String 来说大大提高了性能。
>
> **第四，字符串常量池。** Java 使用常量池来存储相同内容的字符串，让它们共享同一个引用，节省内存。如果 String 可变，常量池中的字符串被修改会影响所有引用，产生不可预期的 bug。
>
> **第五，类加载安全。** 类名、包名等都用 String 存储，不可变性确保类加载器不会被欺骗加载错误的类。
>
> 此外，JDK 9 还对 String 进行了优化，将内部存储从 char[] 改为 byte[]，对于纯 ASCII 字符串可以节省一半内存。

---

*点击查看相关代码示例：[StringImmutableDemo.java](../code/StringImmutableDemo.java)*

*本文档持续更新，建议收藏备用*