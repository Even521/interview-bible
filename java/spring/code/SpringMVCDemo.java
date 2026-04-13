package com.example.spring.mvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Min;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring MVC 完整示例
 * 包含RESTful API、参数绑定、拦截器、统一异常处理等
 */
@SpringBootApplication
public class SpringMVCDemo implements WebMvcConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(SpringMVCDemo.class, args);
    }

    // 注册拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/public/**", "/api/login");
    }
}

/**
 * 用户RESTful控制器
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    // 模拟数据库
    private final Map<Long, User> userStore = new ConcurrentHashMap<>();
    private long idGenerator = 1;

    /**
     * 获取所有用户 - GET /api/users
     * 支持分页和排序参数
     */
    @GetMapping
    public ResponseResult<List<User>> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {

        System.out.println("分页参数: page=" + page + ", size=" + size + ", sort=" + sort);

        List<User> users = new ArrayList<>(userStore.values());
        return ResponseResult.success(users);
    }

    /**
     * 根据ID获取用户 - GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseResult<User> getUserById(@PathVariable Long id) {
        User user = userStore.get(id);
        if (user == null) {
            throw new UserNotFoundException("用户不存在: " + id);
        }
        return ResponseResult.success(user);
    }

    /**
     * 创建用户 - POST /api/users
     */
    @PostMapping
    public ResponseResult<User> createUser(@RequestBody @Valid UserDTO userDTO) {
        User user = new User();
        user.setId(idGenerator++);
        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());
        user.setAge(userDTO.getAge());
        user.setCreateTime(new Date());

        userStore.put(user.getId(), user);

        return ResponseResult.success(user, "用户创建成功");
    }

    /**
     * 更新用户 - PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseResult<User> updateUser(
            @PathVariable Long id,
            @RequestBody @Valid UserDTO userDTO) {

        User existingUser = userStore.get(id);
        if (existingUser == null) {
            throw new UserNotFoundException("用户不存在: " + id);
        }

        existingUser.setName(userDTO.getName());
        existingUser.setEmail(userDTO.getEmail());
        existingUser.setAge(userDTO.getAge());
        existingUser.setUpdateTime(new Date());

        return ResponseResult.success(existingUser, "用户更新成功");
    }

    /**
     * 删除用户 - DELETE /api/users/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseResult<Void> deleteUser(@PathVariable Long id) {
        if (!userStore.containsKey(id)) {
            throw new UserNotFoundException("用户不存在: " + id);
        }
        userStore.remove(id);
        return ResponseResult.success(null, "用户删除成功");
    }

    /**
     * 批量删除 - DELETE /api/users/batch
     */
    @DeleteMapping("/batch")
    public ResponseResult<Void> batchDelete(@RequestParam List<Long> ids) {
        ids.forEach(userStore::remove);
        return ResponseResult.success(null, "批量删除成功");
    }

    /**
     * 搜索用户 - GET /api/users/search?name=xxx&minAge=18
     */
    @GetMapping("/search")
    public ResponseResult<List<User>> searchUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false, defaultValue = "0") int minAge) {

        List<User> result = new ArrayList<>();
        for (User user : userStore.values()) {
            boolean match = true;
            if (name != null && !user.getName().contains(name)) {
                match = false;
            }
            if (user.getAge() < minAge) {
                match = false;
            }
            if (match) {
                result.add(user);
            }
        }
        return ResponseResult.success(result);
    }
}

/**
 * 登录控制器 - 公开API
 */
@RestController
@RequestMapping("/api")
public class LoginController {

    @PostMapping("/login")
    public ResponseResult<Map<String, String>> login(
            @RequestParam String username,
            @RequestParam String password) {

        // 模拟登录验证
        if ("admin".equals(username) && "123456".equals(password)) {
            Map<String, String> data = new HashMap<>();
            data.put("token", "jwt-token-123456");
            data.put("username", username);
            return ResponseResult.success(data, "登录成功");
        }
        return ResponseResult.error(401, "用户名或密码错误");
    }

    @GetMapping("/public/info")
    public ResponseResult<String> getPublicInfo() {
        return ResponseResult.success("这是公开信息");
    }
}

/**
 * 统一响应结果封装
 */
class ResponseResult<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp;

    public ResponseResult() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> ResponseResult<T> success(T data) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(200);
        result.setMessage("成功");
        result.setData(data);
        return result;
    }

    public static <T> ResponseResult<T> success(T data, String message) {
        ResponseResult<T> result = success(data);
        result.setMessage(message);
        return result;
    }

    public static <T> ResponseResult<T> error(int code, String message) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    // Getters and Setters
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public long getTimestamp() { return timestamp; }
}

/**
 * 用户DTO - 用于接收请求参数
 */
class UserDTO {
    @NotBlank(message = "用户名不能为空")
    private String name;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Min(value = 0, message = "年龄不能为负数")
    private int age;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
}

/**
 * 用户实体类
 */
class User {
    private Long id;
    private String name;
    private String email;
    private int age;
    private Date createTime;
    private Date updateTime;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}

/**
 * 全局异常处理器
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseResult<Void> handleUserNotFound(UserNotFoundException e) {
        return ResponseResult.error(404, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseResult<Void> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseResult.error(400, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseResult<Void> handleException(Exception e) {
        e.printStackTrace();
        return ResponseResult.error(500, "服务器内部错误");
    }
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
 * 认证拦截器
 */
class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未认证\"}");
            return false;
        }
        // 验证token逻辑...
        System.out.println("拦截器: 验证Token - " + token);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        System.out.println("拦截器: 控制器执行完毕");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        System.out.println("拦截器: 请求处理完成");
    }
}
