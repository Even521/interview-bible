package com.example.springboot.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Min;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Spring Boot 完整示例应用
 * 演示常用注解和功能特性
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class SpringBootDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootDemoApplication.class, args);
        System.out.println("=== Spring Boot Demo 启动成功 ===");
    }
}

/**
 * REST API 控制器
 * 演示各种请求映射和参数绑定
 */
@RestController
@RequestMapping("/api/v1")
@Validated
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取所有用户 - GET /api/v1/users
     */
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userService.findAll();
    }

    /**
     * 根据ID获取用户 - GET /api/v1/users/{id}
     */
    @GetMapping("/users/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .orElseThrow(() -> new UserNotFoundException("用户不存在: " + id));
    }

    /**
     * 创建用户 - POST /api/v1/users
     */
    @PostMapping("/users")
    public User createUser(@RequestBody @Valid UserDTO userDTO) {
        return userService.save(userDTO.toUser());
    }

    /**
     * 更新用户 - PUT /api/v1/users/{id}
     */
    @PutMapping("/users/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody @Valid UserDTO userDTO) {
        return userService.update(id, userDTO.toUser());
    }

    /**
     * 删除用户 - DELETE /api/v1/users/{id}
     */
    @DeleteMapping("/users/{id}")
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
    }

    /**
     * 搜索用户 - GET /api/v1/users/search?name=xxx
     */
    @GetMapping("/users/search")
    public List<User> searchUsers(@RequestParam String name) {
        return userService.searchByName(name);
    }

    /**
     * 异步获取用户统计 - GET /api/v1/users/stats
     */
    @GetMapping("/users/stats")
    public CompletableFuture<UserStats> getUserStats() {
        return userService.calculateStatsAsync();
    }
}

/**
 * 服务层
 * 演示业务逻辑、缓存和异步处理
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * 查找所有用户
     */
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * 根据ID查找 - 带缓存
     */
    @Cacheable(value = "users", key = "#id", unless = "#result == null")
    public Optional<User> findById(Long id) {
        simulateSlowService();
        return userRepository.findById(id);
    }

    /**
     * 保存用户
     */
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * 更新用户
     */
    public User update(Long id, User user) {
        return userRepository.findById(id)
                .map(existing -> {
                    existing.setName(user.getName());
                    existing.setEmail(user.getEmail());
                    existing.setAge(user.getAge());
                    return userRepository.save(existing);
                })
                .orElseThrow(() -> new UserNotFoundException("用户不存在: " + id));
    }

    /**
     * 搜索用户
     */
    public List<User> searchByName(String name) {
        return userRepository.findByNameContaining(name);
    }

    /**
     * 删除用户
     */
    @CacheEvict(value = "users", key = "#id")
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    /**
     * 异步计算统计
     */
    @Async
    public CompletableFuture<UserStats> calculateStatsAsync() {
        long count = userRepository.count();
        double avgAge = userRepository.findAll().stream()
                .mapToInt(User::getAge)
                .average()
                .orElse(0.0);

        return CompletableFuture.completedFuture(new UserStats(count, avgAge));
    }

    /**
     * 模拟慢查询
     */
    private void simulateSlowService() {
        try {
            Thread.sleep(1000L); // 模拟1秒延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * 数据访问层 - JPA Repository
 */
// @Repository // 实际项目中需要引入Spring Data JPA
interface UserRepository /* extends JpaRepository<User, Long> */ {

    List<User> findAll();
    Optional<User> findById(Long id);
    User save(User user);
    void deleteById(Long id);
    long count();

    // 自定义查询
    List<User> findByNameContaining(String name);
}

/**
 * 用户实体类
 */
@Document(collection = "users") // MongoDB注解，JPA使用@Entity
class User {

    @Id
    private Long id;

    @NotBlank(message = "用户名不能为空")
    private String name;

    @NotBlank(message = "邮箱不能为空")
    private String email;

    @Min(value = 1, message = "年龄必须大于0")
    private int age;

    // 构造器
    public User() {}

    public User(Long id, String name, String email, int age) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", age=" + age +
                '}';
    }
}

/**
 * 用户DTO（数据传输对象）
 */
class UserDTO {

    @NotBlank(message = "用户名不能为空")
    private String name;

    @NotBlank(message = "邮箱不能为空")
    private String email;

    @Min(value = 1, message = "年龄必须大于0")
    private int age;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    /**
     * 转换为User实体
     */
    public User toUser() {
        return new User(null, name, email, age);
    }
}

/**
 * 用户统计信息
 */
class UserStats {
    private long totalCount;
    private double averageAge;

    public UserStats(long totalCount, double averageAge) {
        this.totalCount = totalCount;
        this.averageAge = averageAge;
    }

    // Getters
    public long getTotalCount() { return totalCount; }
    public double getAverageAge() { return averageAge; }
}

/**
 * 自定义异常
 */
class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}

/**
 * 全局异常处理器
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ErrorResponse handleUserNotFound(UserNotFoundException ex) {
        return new ErrorResponse(404, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse handleGenericException(Exception ex) {
        return new ErrorResponse(500, "服务器内部错误: " + ex.getMessage());
    }
}

/**
 * 错误响应
 */
class ErrorResponse {
    private int code;
    private String message;
    private long timestamp;

    public ErrorResponse(int code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public int getCode() { return code; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
}

/**
 * 配置类
 */
@Configuration
class AppConfig {

    @Bean
    public String appName(@Value("${spring.application.name}") String name) {
        System.out.println("应用名称: " + name);
        return name;
    }
}
