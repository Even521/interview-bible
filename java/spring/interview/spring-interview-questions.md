interview-bible\java\spring\interview\spring-interview-questions.md
# Spring 面试题大全

## 目录
1. [Spring Core](#spring-core)
2. [Spring IoC/DI](#spring-iocdi)
3. [Spring AOP](#spring-aop)
4. [Spring MVC](#spring-mvc)
5. [Spring Boot](#spring-boot)
6. [Spring Data JPA](#spring-data-jpa)
7. [Spring Security](#spring-security)
8. [Spring Cloud](#spring-cloud)
9. [Spring Transaction](#spring-transaction)

---

## Spring Core

### 1. 什么是Spring框架？

**答案：**
Spring是一个轻量级的开源Java框架，旨在简化企业级应用开发。它提供了全面的基础设施支持，使开发者可以专注于应用的业务逻辑，而不需要过多关注底层技术实现。

**核心特性：**
- 轻量级、非侵入式
- 控制反转（IoC）
- 面向切面编程（AOP）
- 声明式事务管理
- 与各种框架的集成支持

### 2. Spring框架的主要模块有哪些？

**答案：**
```
Spring Core Container
├── Core: IoC和依赖注入的基本实现
├── Beans: Bean工厂支持
├── Context: 应用程序上下文
└── Expression Language: SpEL表达式语言

Spring AOP: 面向切面编程

Spring JDBC: JDBC抽象层

Spring ORM: ORM框架集成（Hibernate、JPA等）

Spring Web: Web应用支持

Spring MVC: Web MVC框架

Spring Test: 测试支持
```

---

## Spring IoC/DI

### 3. 什么是IoC（控制反转）？

**答案：**
IoC（Inversion of Control）是一种设计原则，将对象的创建和管理权从程序代码中转移到外部容器。

**传统方式 vs IoC方式：**

```java
// 传统方式 - 对象自己创建依赖
public class UserService {
    private UserDao userDao = new UserDaoImpl(); // 紧耦合
}

// IoC方式 - 依赖由外部注入
public class UserService {
    private UserDao userDao; // 通过构造器或setter注入

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }
}
```

### 4. 什么是DI（依赖注入）？

**答案：**
DI（Dependency Injection）是IoC的一种实现方式，通过构造器、Setter方法或字段注入的方式将依赖对象注入到目标对象中。

**三种注入方式：**

```java
// 1. 构造器注入（推荐）
@Service
public class UserService {
    private final UserDao userDao;

    @Autowired
    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }
}

// 2. Setter注入
@Service
public class UserService {
    private UserDao userDao;

    @Autowired
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
}

// 3. 字段注入
@Service
public class UserService {
    @Autowired
    private UserDao userDao;
}
```

### 5. @Autowired 和 @Resource 的区别？

**答案：**

| 特性 | @Autowired | @Resource |
|------|------------|-----------|
| 来源 | Spring提供 | JSR-250标准（Java提供） |
| 注入方式 | 默认按类型 | 默认按名称 |
| 指定名称 | @Qualifier | name属性 |
| 适用范围 | 构造器、方法、参数、字段 | 字段、Setter |

```java
// @Autowired 示例
@Autowired
@Qualifier("myUserDao") // 指定具体bean
private UserDao userDao;

// @Resource 示例
@Resource(name = "myUserDao") // 直接指定名称
private UserDao userDao;
```

### 6. Spring Bean的生命周期？

**答案：**

```
1. 实例化 (Instantiation)
 ↓
2. 属性赋值 (Populate Properties)
 ↓
3. BeanNameAware.setBeanName()
 ↓
4. BeanFactoryAware.setBeanFactory()
 ↓
5. ApplicationContextAware.setApplicationContext()
 ↓
6. @PostConstruct / InitializingBean.afterPropertiesSet()
 ↓
7. 自定义初始化方法 (init-method)
 ↓
8. Bean就绪使用
 ↓
9. @PreDestroy / DisposableBean.destroy()
 ↓
10. 自定义销毁方法 (destroy-method)
```

```java
@Component
public class MyBean implements InitializingBean, DisposableBean {

    @PostConstruct
    public void postConstruct() {
        System.out.println("@PostConstruct 执行");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("InitializingBean.afterPropertiesSet() 执行");
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("@PreDestroy 执行");
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("DisposableBean.destroy() 执行");
    }
}
```

### 7. Spring Bean的作用域？

**答案：**

| 作用域 | 说明 |
|--------|------|
| singleton | 默认，每个Spring容器只有一个实例 |
| prototype | 每次获取都创建新实例 |
| request | HTTP请求生命周期（Web环境）|
| session | HTTP会话生命周期（Web环境）|
| application | ServletContext生命周期 |
| websocket | WebSocket生命周期 |

```java
@Component
@Scope("prototype") // 指定作用域
public class PrototypeBean {
    // ...
}
```

### 8. Spring如何解决循环依赖？

**答案：**
Spring通过三级缓存解决单例Bean的循环依赖：

```
三级缓存结构：
1. singletonObjects: 一级缓存，存放完全初始化好的Bean
2. earlySingletonObjects: 二级缓存，存放早期暴露的Bean（未填充属性）
3. singletonFactories: 三级缓存，存放Bean工厂对象

解决流程：
A创建 → 提前暴露A（放入三级缓存）→ A依赖B → B创建 →
B依赖A → 从三级缓存获取早期A → B完成 → A完成
```

**注意：** 构造器注入的循环依赖无法解决，因为对象还未实例化完成。

---

## Spring AOP

### 9. 什么是AOP？

**答案：**
AOP（Aspect Oriented Programming）面向切面编程，是一种编程范式，用于将横切关注点（日志、事务、权限等）从业务逻辑中分离出来。

**核心概念：**

```
Aspect（切面） → 横切关注点的模块化
Pointcut（切点） → 定义在何处切入
Advice（通知） → 定义何时切入及做什么
 ├── Before: 方法执行前
 ├── After: 方法执行后（无论是否异常）
 ├── AfterReturning: 方法正常返回后
 ├── AfterThrowing: 方法抛出异常后
 └── Around: 包围方法执行
JoinPoint（连接点） → 程序执行过程中的特定点
Target（目标对象） → 被代理的对象
```

### 10. AOP的实现原理？

**答案：**
Spring AOP基于动态代理实现：

**JDK动态代理**
- 基于接口代理
- 要求目标类实现接口
- 使用`java.lang.reflect.Proxy`

**CGLIB代理**
- 基于类代理
- 生成目标类的子类
- 通过继承实现，不能代理final类

```java
// AOP配置示例
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("execution(* com.example.service.*.*(..))")
    public void serviceMethods() {}

    @Before("serviceMethods()")
    public void logBefore(JoinPoint joinPoint) {
        System.out.println("方法调用: " + joinPoint.getSignature());
    }

    @Around("serviceMethods()")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long end = System.currentTimeMillis();
        System.out.println("执行时间: " + (end - start) + "ms");
        return result;
    }
}
```

### 11. AOP的切点表达式？

**答案：**

```java
// execution 表达式语法
execution(修饰符 返回类型 包名.类名.方法名(参数) throws 异常)

// 示例
@Pointcut("execution(public * *(..))") // 所有public方法
@Pointcut("execution(* com.service.*.*(..))") // service包下所有方法
@Pointcut("execution(* com.service..*.*(..))") // service包及其子包
@Pointcut("execution(* com.service.UserService.*(..))") // UserService所有方法
@Pointcut("execution(* com.service.*.save*(..))") // 以save开头的方法
@Pointcut("execution(* com.service.*.*(String, ..))") // 第一个参数是String

// 其他表达式
@Pointcut("@annotation(com.annotation.Log)") // 带特定注解的方法
@Pointcut("within(com.service.*)") // 特定包下的类
@Pointcut("this(com.service.UserService)") // 特定类型的代理对象
@Pointcut("target(com.service.UserService)") // 特定类型的目标对象
@Pointcut("args(java.io.Serializable)") // 参数类型匹配
```

---

## Spring MVC

### 12. Spring MVC的工作流程？

**答案：**

```
1. 客户端发送HTTP请求
 ↓
2. DispatcherServlet 接收请求（前端控制器）
 ↓
3. HandlerMapping 查找合适的处理器
 ↓
4. HandlerAdapter 调用处理器方法
 ↓
5. Controller 执行业务逻辑
 ↓
6. 返回ModelAndView（或@ResponseBody直接返回）
 ↓
7. ViewResolver 解析视图
 ↓
8. 渲染视图并返回响应
```

### 13. @Controller 和 @RestController 的区别？

**答案：**

```java
// @Controller 返回视图
@Controller
public class ViewController {
    @RequestMapping("/hello")
    public String hello(Model model) {
        model.addAttribute("message", "Hello");
        return "hello"; // 解析为视图名
    }

    @RequestMapping("/data")
    @ResponseBody // 需要手动添加
    public String data() {
        return "raw data";
    }
}

// @RestController = @Controller + @ResponseBody
@RestController
public class ApiController {
    @GetMapping("/user")
    public User getUser() { // 自动返回JSON
        return new User("张三", 20);
    }
}
```

### 14. Spring MVC常用注解？

**答案：**

| 注解 | 用途 |
|------|------|
| @RequestMapping | 通用请求映射 |
| @GetMapping | GET请求 |
| @PostMapping | POST请求 |
| @PutMapping | PUT请求 |
| @DeleteMapping | DELETE请求 |
| @RequestParam | 接收查询参数 |
| @PathVariable | 接收路径变量 |
| @RequestBody | 接收请求体（JSON）|
| @ResponseBody | 返回JSON/XML |
| @ModelAttribute | 绑定模型属性 |

```java
@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/{id}")
    public User getById(@PathVariable Long id,
                        @RequestParam(required = false) String detail) {
        // /users/1?detail=full
        return userService.findById(id);
    }

    @PostMapping
    public ResponseEntity<User> create(@RequestBody @Valid UserDTO userDTO) {
        User user = userService.save(userDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
}
```

---

## Spring Boot

### 15. Spring Boot的自动配置原理？

**答案：**
Spring Boot自动配置通过`@EnableAutoConfiguration`实现：

```
1. @SpringBootApplication
   ├── @Configuration → 标记为配置类
   ├── @EnableAutoConfiguration → 启用自动配置
   │   └── 扫描 META-INF/spring.factories
   │   └── 加载所有 AutoConfiguration 类
   │   └── 根据条件注解（@Conditional）判断是否生效
   └── @ComponentScan → 组件扫描
```

**条件注解：**

```java
@ConditionalOnClass // 类存在时配置
@ConditionalOnMissingClass // 类不存在时配置
@ConditionalOnBean // Bean存在时配置
@ConditionalOnMissingBean // Bean不存在时配置
@ConditionalOnProperty // 配置属性匹配时
@ConditionalOnWebApplication // Web应用时
```

### 16. Spring Boot Starter的作用？

**答案：**
Starter是一组便利的依赖描述符，简化Maven/Gradle配置。

```
spring-boot-starter-web → Web开发（Spring MVC, Tomcat）
spring-boot-starter-data-jpa → JPA支持
spring-boot-starter-security → 安全支持
spring-boot-starter-test → 测试支持
spring-boot-starter-redis → Redis支持
spring-boot-starter-amqp → RabbitMQ支持
```

### 17. Spring Boot配置文件加载顺序？

**答案：**
优先级从高到低：

```
1. 命令行参数: --server.port=8081
2. Java系统属性: System.getProperties()
3. 环境变量
4. application-{profile}.yml/properties
5. application.yml/properties
6. @PropertySource
7. 默认值
```

```yaml
# application.yml
spring:
  profiles:
    active: dev # 激活dev环境

---
# application-dev.yml
server:
  port: 8080

---
# application-prod.yml
server:
  port: 80
```

### 18. Spring Boot Actuator？

**答案：**
Actuator提供生产就绪功能，用于监控和管理应用。

```yaml
# 配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env,beans
  endpoint:
    health:
      show-details: always
```

**常用端点：**
- `/actuator/health` - 健康检查
- `/actuator/info` - 应用信息
- `/actuator/metrics` - 运行时指标
- `/actuator/env` - 环境属性
- `/actuator/beans` - 所有Spring Bean
- `/actuator/loggers` - 日志配置

---

## Spring Data JPA

### 19. Spring Data JPA的Repository接口？

**答案：**

```java
// 继承关系
Repository<T, ID> // 最顶层接口
 └── CrudRepository<T, ID> // CRUD操作
     └── PagingAndSortingRepository<T, ID> // 分页排序
         └── JpaRepository<T, ID> // JPA特定方法

// 自定义Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 方法名查询（自动实现）
    List<User> findByUsernameAndStatus(String username, Integer status);

    // 自定义JPQL
    @Query("SELECT u FROM User u WHERE u.age > :age")
    List<User> findByAgeGreaterThan(@Param("age") int age);

    // 原生SQL
    @Query(value = "SELECT * FROM user WHERE status = ?1", nativeQuery = true)
    List<User> findByStatusNative(int status);

    // 修改操作
    @Modifying
    @Query("UPDATE User u SET u.status = ?2 WHERE u.id = ?1")
    int updateStatus(Long id, int status);
}
```

### 20. JPA实体状态？

**答案：**

```
瞬时（Transient）: new 创建，未与Session关联
持久（Persistent）: 由Session管理，数据库有对应记录
游离（Detached）: 曾经持久化，现在Session关闭
移除（Removed）: 标记为删除，commit后从数据库删除
```

```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @ManyToOne(fetch = FetchType.LAZY) // 延迟加载
    @JoinColumn(name = "dept_id")
    private Department department;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Order> orders;

    @Enumerated(EnumType.STRING)
    private UserStatus status;
}
```

---

## Spring Security

### 21. Spring Security的核心组件？

**答案：**

```
SecurityContextHolder → 存储安全上下文（当前用户）
SecurityContext → 包含Authentication
Authentication → 认证信息（令牌/凭证/权限）
UserDetails → 用户信息接口
UserDetailsService → 加载用户信息
AuthenticationProvider → 执行认证逻辑
FilterChain → 过滤器链
```

### 22. JWT认证流程？

**答案：**

```
1. 用户登录 → 验证用户名密码
2. 生成JWT → Header.Payload.Signature
3. 返回Token → 客户端存储
4. 后续请求 → Authorization: Bearer <token>
5. 服务端验证 → 解析Token并认证
```

```java
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private long jwtExpiration = 86400000; // 24小时

    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpiration);

        return Jwts.builder()
                .setSubject(userPrincipal.getId())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 无效token
        }
        return false;
    }
}
```

---

## Spring Cloud

### 23. Spring Cloud核心组件？

**答案：**

| 组件 | 功能 | 替代方案 |
|------|------|---------|
| Eureka | 服务注册与发现 | Consul, Nacos |
| Ribbon | 客户端负载均衡 | Spring Cloud LoadBalancer |
| Feign | 声明式HTTP客户端 | OpenFeign |
| Hystrix | 断路器（服务熔断降级）| Resilience4j |
| Zuul/Gateway | API网关 | Spring Cloud Gateway |
| Config | 集中式配置管理 | Apollo, Nacos |
| Sleuth | 分布式链路追踪 | Zipkin |
| Bus | 消息总线 | RabbitMQ/Kafka |

### 24. 服务熔断降级原理？

**答案：**

```
熔断器三种状态：
┌─────────────┐ 失败阈值 ┌──────────┐
│  CLOSED     │ ──────────────→ │   OPEN   │
│ (正常通行)   │                  │ (快速失败)│
└─────────────┘                  └────┬────┘
     ↑                                │
     │         超时时间                │
     └─────────────────────────────────┘
                      ↓
              ┌──────────┐
              │ HALF-OPEN│
              │ (试探放行) │
              └──────────┘

降级策略：
- 失败率达到阈值（如50%）
- 响应时间超过阈值
- 资源隔离（线程池/信号量）
```

```java
// Hystrix示例
@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserClient {
    @GetMapping("/user/{id}")
    User getUserById(@PathVariable("id") Long id);
}

@Component
public class UserClientFallback implements UserClient {
    @Override
    public User getUserById(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("默认用户");
        return user; // 返回默认值
    }
}
```

---

## Spring Transaction

### 25. Spring事务传播行为？

**答案：**

| 传播行为 | 说明 |
|---------|------|
| REQUIRED | 默认，当前有事务则加入，无则新建 |
| SUPPORTS | 有事务则加入，无则以非事务执行 |
| MANDATORY | 必须有事务，否则抛异常 |
| REQUIRES_NEW | 新建事务，挂起当前事务 |
| NOT_SUPPORTED | 以非事务执行，挂起当前事务 |
| NEVER | 必须非事务，否则抛异常 |
| NESTED | 嵌套事务（保存点）|

```java
@Service
public class OrderService {

    @Transactional(propagation = Propagation.REQUIRED)
    public void createOrder(Order order) {
        orderDao.save(order);

        // 需要独立事务
        logService.recordLog(order); // REQUIRES_NEW
    }
}

@Service
public class LogService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLog(Log log) {
        logDao.save(log);
    }
}
```

### 26. Spring事务隔离级别？

**答案：**

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | 说明 |
|---------|------|-----------|------|------|
| READ_UNCOMMITTED | ✓ | ✓ | ✓ | 最低级别 |
| READ_COMMITTED | ✗ | ✓ | ✓ | Oracle默认 |
| REPEATABLE_READ | ✗ | ✗ | ✓ | MySQL默认 |
| SERIALIZABLE | ✗ | ✗ | ✗ | 最高级别 |

```java
@Transactional(isolation = Isolation.READ_COMMITTED,
               timeout = 30,
               rollbackFor = Exception.class)
public void transfer(Account from, Account to, BigDecimal amount) {
    // 转账逻辑
}
```

### 27. @Transactional注解失效的场景？

**答案：**

```
1. 非public方法
2. 同类内方法调用（this调用）
3. 异常被捕获未抛出
4. 错误异常类型（默认只回滚RuntimeException）
5. 多线程环境
6. 数据库引擎不支持（如MyISAM）

解决方案：
1. 使用AopContext.currentProxy()
2. 注入自身代理
3. 拆分到另一个类
```

```java
@Service
public class OrderService {

    @Autowired
    private OrderService self; // 注入自身代理

    public void outerMethod() {
        // 不会触发事务
        innerMethod();

        // 使用代理调用，触发事务
        self.innerMethod();
    }

    @Transactional
    public void innerMethod() {
        // 业务逻辑
    }
}
```

---

## 致谢

本文档涵盖了Spring框架的核心面试知识点，建议结合实际代码加深理解。代码示例详见`code`目录。