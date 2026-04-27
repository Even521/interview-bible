4. [Spring 如何选择代理方式](#spring-如何选择代理方式)
5. [源码分析](#源码分析)
6. [性能对比](#性能对比)
7. [常见问题与陷阱](#常见问题与陷阱)

---

## AOP 代理概述

### 什么是AOP代理？

Spring AOP 使用**代理模式**实现切面功能。当目标 Bean 需要被增强时，Spring 会创建一个代理对象来包装目标对象，客户端实际使用的是代理对象而非原始目标对象。

```
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│   客户端调用     │ ───→ │   代理对象       │ ───→ │   目标对象       │
│                 │      │ (Proxy/CGLIB)   │      │  (Target)       │
└─────────────────┘      └─────────────────┘      └─────────────────┘
                                │
                                ↓
                         ┌─────────────────┐
                         │   增强逻辑       │
                         │ (前置/后置/环绕)  │
                         └─────────────────┘
```

### 两种代理方式

| 代理方式 | 实现原理 | 要求 |
|---------|---------|------|
| JDK 动态代理 | 反射 + 接口 | 目标类必须实现接口 |
| CGLIB 代理 | ASM 字节码生成 + 继承 | 目标类不能是 final |

---

## JDK 动态代理

### 核心原理

基于 Java 原生 `java.lang.reflect.Proxy` 类，在运行时动态创建接口的实现类。

```java
public class JDKProxyDemo {
    
    // 目标接口（必须）
    interface UserService {
        void addUser(String name);
        String getUserById(Long id);
    }
    
    // 目标实现类
    static class UserServiceImpl implements UserService {
        @Override
        public void addUser(String name) {
            System.out.println("添加用户: " + name);
        }
        
        @Override
        public String getUserById(Long id) {
            return "User-" + id;
        }
    }
    
    // 调用处理器
    static class MyInvocationHandler implements InvocationHandler {
        private final Object target;
        
        public MyInvocationHandler(Object target) {
            this.target = target;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 前置增强
            System.out.println("[JDK代理] 方法调用前: " + method.getName());
            
            // 执行目标方法
            long start = System.currentTimeMillis();
            Object result = method.invoke(target, args);
            long end = System.currentTimeMillis();
            
            // 后置增强
            System.out.println("[JDK代理] 方法调用后, 耗时: " + (end - start) + "ms");
            
            return result;
        }
    }
    
    public static void main(String[] args) {
        // 创建目标对象
        UserService target = new UserServiceImpl();
        
        // 创建代理对象
        UserService proxy = (UserService) Proxy.newProxyInstance(
            target.getClass().getClassLoader(),    // 类加载器
            target.getClass().getInterfaces(),     // 代理的接口数组
            new MyInvocationHandler(target)        // 调用处理器
        );
        
        // 调用代理
        proxy.addUser("张三");
    }
}
```

### 生成的代理类结构

```java
// 代理类伪代码（运行时动态生成）
public final class $Proxy0 extends Proxy implements UserService {
    
    private static Method addUserMethod;
    private static Method getUserByIdMethod;
    private InvocationHandler h;  // 我们的调用处理器
    
    public $Proxy0(InvocationHandler h) {
        super(h);
    }
    
    @Override
    public void addUser(String name) {
        // 转发到 InvocationHandler.invoke()
        h.invoke(this, addUserMethod, new Object[]{name});
    }
    
    @Override
    public String getUserById(Long id) {
        return (String) h.invoke(this, getUserByIdMethod, new Object[]{id});
    }
}
```

### 关键特点

**✅ 优点**
- JDK 内置，无需额外依赖
- 面向接口编程，符合设计原则
- 生成速度较快

**❌ 局限**
- 必须实现接口
- 只能代理接口定义的方法
- 反射调用有一定性能开销

---

## CGLIB 代理

### 核心原理

基于 ASM 字节码库，在运行时生成目标类的子类，通过方法重写实现代理。

```java
import net.sf.cglib.proxy.*;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

public class CGLIBProxyDemo {
    
    // 目标类（不需要实现接口）
    static class UserService {
        public void addUser(String name) {
            System.out.println("添加用户: " + name);
        }
        
        // final方法无法被代理
        public final void deleteUser(Long id) {
            System.out.println("删除用户: " + id);
        }
        
        // protected方法可以被代理
        protected void updateUser(String name) {
            System.out.println("更新用户: " + name);
        }
    }
    
    // 方法拦截器
    static class MyMethodInterceptor implements MethodInterceptor {
        
        @Override
        public Object intercept(Object obj, Method method, Object[] args, 
                               MethodProxy proxy) throws Throwable {
            // 前置增强
            System.out.println("[CGLIB代理] 拦截方法: " + method.getName());
            
            // 执行目标方法（使用MethodProxy更快）
            long start = System.currentTimeMillis();
            Object result = proxy.invokeSuper(obj, args);  // 调用父类方法
            long end = System.currentTimeMillis();
            
            // 后置增强
            System.out.println("[CGLIB代理] 方法执行完成, 耗时: " + (end - start) + "ms");
            
            return result;
        }
    }
    
    public static void main(String[] args) {
        // 创建Enhancer
        Enhancer enhancer = new Enhancer();
        
        // 设置父类
        enhancer.setSuperclass(UserService.class);
        
        // 设置回调
        enhancer.setCallback(new MyMethodInterceptor());
        
        // 创建代理对象
        UserService proxy = (UserService) enhancer.create();
        
        // 调用代理
        proxy.addUser("李四");
        
        // 无法被代理的方法
        proxy.deleteUser(1L);  // 直接调用父类方法，无增强
    }
}
```

### 生成的代理类结构

```java
// CGLIB生成的代理类（大概结构）
public class UserService$$EnhancerByCGLIB$$xxx extends UserService {
    
    private MethodInterceptor callback;
    private static MethodProxy addUserProxy;
    
    @Override
    public void addUser(String name) {
        if (callback != null) {
            // 通过MethodInterceptor增强
            callback.intercept(this, addUserMethod, 
                new Object[]{name}, addUserProxy);
        } else {
            super.addUser(name);
        }
    }
    
    // 无法重写final方法
    // public final void deleteUser(Long id) { ... }
}
```

### 关键特点

**✅ 优点**
- 无需实现接口
- 可以代理类的大多数方法
- MethodProxy 比反射性能更好

**❌ 局限**
- 需要引入 CGLIB 库
- 不能代理 final 类
- 不能代理 final 方法
- 生成字节码较慢，占用更多内存

---

## Spring 如何选择代理方式

### 选择策略

Spring 默认根据以下条件选择代理方式：

```java
// 伪代码表示选择逻辑
if (目标类实现了接口 && 没有强制使用CGLIB) {
    使用 JDK 动态代理;
} else {
    使用 CGLIB 代理;
}
```

### 配置方式

**方式一：全局配置（推荐）**

```java
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)  // 强制使用CGLIB
public class AopConfig {
}
```

**方式二：XML配置**

```xml
<!-- 默认，使用JDK代理（需实现接口） -->
<aop:aspectj-autoproxy />

<!-- 强制使用CGLIB代理 -->
<aop:aspectj-autoproxy proxy-target-class="true" />
```

**方式三：Spring Boot 配置**

```yaml
# application.yml
spring:
  aop:
    proxy-target-class: true  # Spring Boot 2.x 默认为 true
```

### 版本差异

| Spring 版本 | 默认行为 |
|------------|---------|
| Spring < 4.x | 默认 JDK 代理 |
| Spring 4.x - 5.x | 默认 JDK 代理（有接口时）|
| Spring Boot 1.x | 默认 JDK 代理 |
| Spring Boot 2.x+ | **默认 CGLIB** (`proxyTargetClass: true`)|

---

## 源码分析

### 关键类：DefaultAopProxyFactory

```java
// Spring 源码简化版
public class DefaultAopProxyFactory implements AopProxyFactory {
    
    @Override
    public AopProxy createAopProxy(AdvisedSupport config) {
        // 1. 检查是否优化或强制使用CGLIB
        if (config.isOptimize() || config.isProxyTargetClass() 
                || hasNoUserSuppliedProxyInterfaces(config)) {
            
            Class<?> targetClass = config.getTargetClass();
            
            // 2. 目标类是接口或代理类本身，使用JDK代理
            if (targetClass.isInterface() || 
                Proxy.isProxyClass(targetClass)) {
                return new JdkDynamicAopProxy(config);
            }
            
            // 3. 否则使用CGLIB
            return new ObjenesisCglibAopProxy(config);
        }
        
        // 4. 默认使用JDK动态代理
        return new JdkDynamicAopProxy(config);
    }
}
```

### JDK 代理核心：JdkDynamicAopProxy

```java
// 简化版源码
public class JdkDynamicAopProxy implements AopProxy, InvocationHandler {
    
    private final AdvisedSupport advised;
    
    @Override
    public Object getProxy(ClassLoader classLoader) {
        return Proxy.newProxyInstance(classLoader, 
            advised.getProxiedInterfaces(), this);
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        // 1. 判断是否需要进行AOP处理
        MethodInvocation invocation = new ReflectiveMethodInvocation(
            advised.getTargetSource().getTarget(),
            method, args, advised.getMethodInterceptorList()
        );
        
        // 2. 执行拦截器链
        return invocation.proceed();
    }
}
```

### CGLIB 代理核心：CglibAopProxy

```java
// 简化版源码
public class CglibAopProxy implements AopProxy {
    
    @Override
    public Object getProxy(ClassLoader classLoader) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(advised.getTargetClass());
        enhancer.setCallback(new DynamicAdvisedInterceptor());
        return enhancer.create();
    }
    
    // CGLIB回调类
    private class DynamicAdvisedInterceptor implements MethodInterceptor {
        @Override
        public Object intercept(Object obj, Method method, 
                Object[] args, MethodProxy proxy) {
            // 处理拦截器链
            return new CglibMethodInvocation(
                advised.getTarget(), method, args, proxy, advisors
            ).proceed();
        }
    }
}
```

---

## 性能对比

### 测试结果（仅供参考）

| 代理方式 | 对象创建时间 | 方法调用性能 | 内存占用 |
|---------|-------------|-------------|---------|
| 原生对象 | 基准 | 基准（最快）| 基准 |
| JDK 代理 | 较快 | 较慢（反射）| 较小 |
| CGLIB 代理 | 较慢 | 较快（FastClass）| 较大 |

> **注意**：Spring 4 之后对 JDK 代理进行了优化，两者性能差距已经很小。大多数情况下不必过于关注。

### 性能优化建议

1. **缓存代理对象**：避免重复创建代理
2. **合理设置代理范围**：缩小切点匹配范围
3. **避免过度切面**：过多的切面会影响性能
4. **使用 `proxyTargetClass = true`**：Spring Boot 2.x 已默认优化

---

## 常见问题与陷阱

### 问题一：同类调用导致AOP失效

```java
@Service
public class UserService {
    
    public void methodA() {
        this.methodB();  // ❌ 直接调用，不走代理
    }
    
    @Transactional
    public void methodB() {
        // 事务不会生效！
    }
}
```

**解决方案**：

```java
@Service
public class UserService {
    
    @Autowired
    private UserService self;  // 注入自身代理
    
    public void methodA() {
        self.methodB();  // ✅ 通过代理调用
    }
}
```

### 问题二：final 方法无法被代理

```java
@Service
public class UserService {
    
    @Override
    public final String toString() {  // ❌ final方法
        return "UserService";
    }
}
```

### 问题三：内部类访问权限

```java
@Service
public class UserService {
    
    // 包私有方法可以被CGLIB代理
    void packagePrivateMethod() { }
    
    // protected方法可以被CGLIB代理
    protected void protectedMethod() { }
    
    // private方法无法被代理
    private void privateMethod() { }
}
```

### 问题四：循环依赖

```java
// AOP代理可能加剧循环依赖问题
@Service
public class ServiceA {
    @Autowired
    private ServiceB serviceB;  // 循环依赖
}

@Service
public class ServiceB {
    @Autowired
    private ServiceA serviceA;  // 循环依赖
}
```

**解决方案**：使用 `@Lazy` 延迟注入或重构代码消除循环依赖。

---

## 总结

| 场景 | 推荐方案 |
|------|---------|
| 有接口，追求规范 | JDK 动态代理（默认）|
| 无接口，或需代理类方法 | CGLIB 代理 |
| Spring Boot 2.x+ | 默认 CGLIB（推荐）|
| 性能极度敏感 | 原生方式或 AspectJ 编译时织入 |

### 核心要点

1. **Spring AOP 基于代理**，不是真正的字节码修改（AspectJ 才是）
2. **JDK 代理必须实现接口**，基于反射调用
3. **CGLIB 代理基于继承**，不能代理 final 类/方法
4. **同类调用不会触发 AOP**，因为不是通过代理对象
5. **Spring Boot 2.x 默认 CGLIB**，降低使用门槛

### 面试金句

> "Spring AOP 底层使用 JDK 动态代理或 CGLIB 代理。JDK 代理要求目标实现接口，基于反射；CGLIB 基于继承生成子类，使用 FastClass 机制调用，性能稍好但不能代理 final 方法。Spring Boot 2.x 默认使用 CGLIB。"

---

**参考链接**：
- Spring Framework AOP 文档：https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop
- CGLIB 源码：https://github.com/cglib/cglib