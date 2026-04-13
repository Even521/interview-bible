interview-bible\java\spring\code\DependencyInjectionDemo.java
package com.example.spring.di;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Spring 依赖注入完整示例
 * 演示构造器注入、Setter注入、字段注入及各种注解的使用
 */
public class DependencyInjectionDemo {

    public static void main(String[] args) {
        // 使用注解配置类初始化Spring容器
        ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        // 获取UserService并测试
        UserService userService = context.getBean(UserService.class);
        userService.saveUser(new User("张三", "zhangsan@example.com"));

        // 获取OrderService并测试
        OrderService orderService = context.getBean(OrderService.class);
        orderService.createOrder(new Order("ORD001", "商品A"));

        // 关闭容器
        ((AnnotationConfigApplicationContext) context).close();
    }
}

/**
 * Spring配置类
 */
@Configuration
@ComponentScan("com.example.spring.di")
class AppConfig {

    // 手动配置Bean示例
    @Bean
    public DataSource dataSource() {
        DataSource ds = new DataSource();
        ds.setUrl("jdbc:mysql://localhost:3306/test");
        ds.setUsername("root");
        ds.setPassword("password");
        return ds;
    }

    // Bean命名示例
    @Bean(name = "primaryCache")
    public CacheManager cacheManager() {
        return new CacheManager("primary");
    }

    @Bean(name = "secondaryCache")
    public CacheManager secondaryCacheManager() {
        return new CacheManager("secondary");
    }
}

/**
 * 构造器注入示例 - 推荐方式（Spring 4.3+ 可省略@Autowired）
 */
@Service
class UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final DataSource dataSource;

    // 构造器注入 - 官方推荐，保证依赖不可变
    @Autowired
    public UserService(UserRepository userRepository,
                       EmailService emailService,
                       DataSource dataSource) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.dataSource = dataSource;
        System.out.println("UserService 构造器注入完成");
    }

    public void saveUser(User user) {
        System.out.println("\n=== UserService 执行业务 ===");
        userRepository.save(user);
        emailService.sendEmail(user.getEmail(), "注册成功");
    }

    @PostConstruct
    public void init() {
        System.out.println("UserService 初始化完成（@PostConstruct）");
    }

    @PreDestroy
    public void destroy() {
        System.out.println("UserService 销毁（@PreDestroy）");
    }
}

/**
 * Setter注入示例
 */
@Service
class OrderService {

    private OrderRepository orderRepository;
    private CacheManager cacheManager;

    // 无参构造器
    public OrderService() {
        System.out.println("OrderService 实例化");
    }

    // Setter注入
    @Autowired
    public void setOrderRepository(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
        System.out.println("OrderService Setter注入: OrderRepository");
    }

    // 使用@Qualifier指定具体Bean
    @Autowired
    @Qualifier("primaryCache")
    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        System.out.println("OrderService Setter注入: CacheManager");
    }

    public void createOrder(Order order) {
        System.out.println("\n=== OrderService 执行业务 ===");

        // 使用缓存
        cacheManager.put("order:" + order.getOrderNo(), order);

        orderRepository.save(order);
        System.out.println("订单创建成功，已缓存到: " + cacheManager.getName());
    }
}

/**
 * 字段注入示例 - 不推荐，仅用于简单场景或遗留代码
 */
@Service
class NotificationService {

    @Autowired
    private SmsService smsService; // 字段注入

    @Resource(name = "pushService") // 按名称注入
    private PushService pushService;

    public void notifyUser(String phone, String message) {
        System.out.println("\n=== NotificationService 发送通知 ===");
        smsService.sendSms(phone, message);
        pushService.push(phone, message);
    }
}

/**
 * Repository层 - 模拟数据访问
 */
@Repository
class UserRepository {
    public void save(User user) {
        System.out.println("UserRepository: 保存用户 - " + user.getName());
    }
}

@Repository
class OrderRepository {
    public void save(Order order) {
        System.out.println("OrderRepository: 保存订单 - " + order.getOrderNo());
    }
}

/**
 * Service层 - 模拟外部服务
 */
@Service
class EmailService {
    public void sendEmail(String to, String subject) {
        System.out.println("EmailService: 发送邮件到 " + to + " 主题: " + subject);
    }
}

@Service
class SmsService {
    public void sendSms(String phone, String message) {
        System.out.println("SmsService: 发送短信到 " + phone + " 内容: " + message);
    }
}

@Service("pushService") // 指定Bean名称
class PushService {
    public void push(String deviceId, String message) {
        System.out.println("PushService: 推送消息到设备 " + deviceId);
    }
}

/**
 * 其他组件类
 */
class DataSource {
    private String url;
    private String username;
    private String password;

    public void setUrl(String url) { this.url = url; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }

    @PostConstruct
    public void init() {
        System.out.println("DataSource 初始化: " + url);
    }
}

class CacheManager {
    private String name;

    public CacheManager(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void put(String key, Object value) {
        System.out.println("Cache [" + name + "]: 缓存 " + key);
    }
}

/**
 * 实体类
 */
class User {
    private String name;
    private String email;

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
}

class Order {
    private String orderNo;
    private String productName;

    public Order(String orderNo, String productName) {
        this.orderNo = orderNo;
        this.productName = productName;
    }

    public String getOrderNo() { return orderNo; }
    public String getProductName() { return productName; }
}
