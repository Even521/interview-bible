# Java 面试题与代码示例

本目录包含 Java 技术栈的面试题和代码示例，按模块分类整理。

## 📁 目录结构

```
interview-bible/java/
├── README.md                    # 本说明文件
├── core/                        # Java核心基础
│   ├── interview/               # 面试题文档
│   │   ├── 类加载面试题.md
│   │   ├── EqualsHashCode面试题.md
│   │   └── String不可变面试题.md
│   └── code/                    # 代码示例
│       ├── ClassLoaderDemo.java
│       ├── EqualsHashCodeDemo.java
│       └── StringImmutableDemo.java
├── collection/                  # 集合框架
│   ├── interview/
│   │   ├── ArrayListLinkedList面试题.md
│   │   ├── HashMap面试题.md
│   │   ├── ConcurrentHashMap面试题.md
│   │   └── CopyOnWriteArrayList面试题.md
│   └── code/
│       ├── ArrayListVsLinkedListDemo.java
│       ├── HashMapDemo.java
│       ├── ConcurrentHashMapDemo.java
│       └── CopyOnWriteArrayListDemo.java
├── concurrent/                    # 并发编程
│   ├── interview/
│   │   ├── Java多线程面试题.md
│   │   ├── Synchronized锁升级面试题.md
│   │   ├── ThreadLocal面试题.md
│   │   └── 并发包工具类面试题.md
│   └── code/
│       ├── ThreadCreationDemo.java
│       ├── SynchronizedVsLockDemo.java
│       ├── ThreadPoolDemo.java
│       ├── ThreadLocalOOM.java
│       ├── ConcurrentUtilsDemo.java
│       ├── FutureCompletableFutureDemo.java
│       └── ForkJoinPoolDemo.java
├── jvm/                         # JVM虚拟机
│   ├── interview/
│   │   ├── JIT和AOT面试题.md
│   │   └── 线上问题排查面试题.md
│   └── code/
│       ├── JITvsAOTDemo.java
│       └── OnlineTroubleshootingDemo.java
└── spring/                      # Spring框架
    ├── interview/
    │   └── spring-interview-questions.md
    └── code/
        ├── DependencyInjectionDemo.java
        ├── AOPDemo.java
        ├── TransactionDemo.java
        ├── BeanLifecycleDemo.java
        ├── SpringMVCDemo.java
        └── SpringBootDemoApplication.java
```

## 📚 模块说明

### 🎯 Core（核心基础）
- **类加载机制** - 类加载器、双亲委派模型、自定义类加载器
- **String不可变性** - 字符串常量池、StringBuilder/StringBuffer
- **Equals和HashCode** - 重写规则、HashMap中的重要性

### 📦 Collection（集合框架）
- **HashMap** - 底层原理、扩容机制、JDK8优化、ConcurrentHashMap区别
- **ArrayList vs LinkedList** - 底层结构、使用场景、性能对比
- **ConcurrentHashMap** - 分段锁、CAS操作、线程安全实现
- **CopyOnWriteArrayList** - 写时复制机制、适用场景

### ⚡ Concurrent（并发编程）
- **Java多线程基础** - 线程创建、生命周期、线程池
- **Synchronized锁升级** - 偏向锁、轻量级锁、重量级锁
- **ThreadLocal** - 原理、内存泄漏问题、使用场景
- **并发工具类** - CountDownLatch、CyclicBarrier、Semaphore、CompletableFuture

### ☕ JVM（虚拟机）
- **JIT与AOT** - 编译优化、GraalVM
- **线上问题排查** - OOM分析、CPU飙高、死锁定位

### 🌱 Spring（框架）
- **依赖注入** - IoC容器、构造器/Setter/字段注入
- **AOP面向切面编程** - 动态代理、切面编程、通知类型
- **Bean生命周期** - 11步完整生命周期、Aware接口
- **事务管理** - 7种传播行为、隔离级别、事务失效场景
- **Spring MVC** - RESTful API、拦截器、统一异常处理
- **Spring Boot** - 自动配置、Starter、缓存、异步、Actuator

## 🚀 学习方式建议

1. **先看面试题** - 了解核心知识点和常见问题
2. **运行代码示例** - 通过实际代码加深理解
3. **动手实践** - 修改代码，观察不同结果
4. **总结提炼** - 形成自己的知识框架

## 📝 使用指南

### 面试题目录（interview/）
包含 Markdown 格式的面试题文档，涵盖：
- 核心概念解释
- 原理分析
- 常见陷阱
- 最佳实践

### 代码示例目录（code/）
包含可直接运行的 Java 代码，演示：
- 核心API使用
- 底层原理实现
- 实际场景案例
- 性能对比测试

## ⚠️ 注意事项

- 代码示例基于 JDK 8+，建议使用 Java 8 或更高版本运行
- Spring 相关代码需要 Maven/Gradle 依赖支持
- JVM 示例请在理解后谨慎运行，避免影响系统稳定性

---

**持续更新中...** 欢迎补充和反馈！