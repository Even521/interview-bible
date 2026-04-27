
## 目录
1. [整体架构](#整体架构)
2. [核心组件](#核心组件)
3. [请求处理流程](#请求处理流程)
4. [源码分析](#源码分析)
5. [常用注解映射](#常用注解映射)
6. [异常处理流程](#异常处理流程)
7. [拦截器机制](#拦截器机制)
8. [最佳实践](#最佳实践)

---

## 整体架构

Spring MVC 采用经典的**前端控制器模式（Front Controller Pattern）**，所有请求都经过 `DispatcherServlet` 统一处理后再分发给具体的控制器。

```
┌─────────────────────────────────────────────────────────────────┐
│                         客户端请求                               │
└──────────────────────────┬──────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│                    DispatcherServlet                            │
│                    (前端控制器)                                   │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐           │
│  │ Handler     │ → │ Handler     │ → │ Handler     │           │
│  │ Mapping     │   │ Adapter     │   │ Execution   │           │
│  │ (处理器映射) │   │ (处理器适配) │   │ (处理器执行) │           │
│  └─────────────┘   └─────────────┘   └─────────────┘           │
│         ↓                                                 ↓     │
│  ┌─────────────┐                                   ┌─────────┐│
│  │ Interceptor │                                   │ View    ││
│  │ (拦截器链)   │                                   │ Resolver││
│  └─────────────┘                                   │(视图解析)││
│                                                   └─────────┘│
└─────────────────────────────────────────────────────────────────┘
                           ↓
                    渲染视图并响应
```

---

## 核心组件

### 1. DispatcherServlet（前端控制器）

```java
// web.xml 配置（传统方式）
<servlet>
    <servlet-name>dispatcher</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
    <init-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>/WEB-INF/spring-mvc.xml</param-value>
    </init-param>
    <load-on-startup>1</load-on-startup>
</servlet>

<servlet-mapping>
    <servlet-name>dispatcher</servlet-name>
    <url-pattern>/</url-pattern>
</servlet-mapping>
```

```java
// Spring Boot 自动配置
@SpringBootApplication
public class SpringMvcApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringMvcApplication.class, args);
    }
}
// Spring Boot 自动配置 DispatcherServlet，映射路径为 "/"
```

### 2. HandlerMapping（处理器映射）

负责将请求 URL 映射到对应的处理器（Controller）。

```java
// 常见的 HandlerMapping 实现
1. RequestMappingHandlerMapping    // 处理 @RequestMapping 注解（最常用）
2. BeanNameUrlHandlerMapping       // 根据 Bean 名称映射
3. SimpleUrlHandlerMapping         // 简单 URL 配置映射
```

### 3. HandlerAdapter（处理器适配器）

```java
// 常见的 HandlerAdapter 实现
1. RequestMappingHandlerAdapter    // 处理 @RequestMapping 方法（最常用）
2. HttpRequestHandlerAdapter       // 处理 HttpRequestHandler 类型
3. SimpleControllerHandlerAdapter  // 处理 Controller 接口类型
```

### 4. ViewResolver（视图解析器）

```java
// 常见的 ViewResolver 实现
1. InternalResourceViewResolver   // JSP 视图解析
2. ThymeleafViewResolver          // Thymeleaf 模板解析
3. FreeMarkerViewResolver          // FreeMarker 模板解析
```

---

## 请求处理流程

### 完整流程图（11步）

```
Step 1    ┌─────────────────┐
────────→ │  HTTP Request   │
          └────────┬────────┘
                   ↓
Step 2    ┌─────────────────────────┐
────────→ │    DispatcherServlet    │
          │    doDispatch() 方法    │
          └────────┬────────────────┘
                   ↓
Step 3    ┌─────────────────────────┐
────────→ │    getHandler()         │
          │    调用 HandlerMapping    │
          │    获取 HandlerExecutionChain│
          └────────┬────────────────┘
                   ↓
Step 4    ┌─────────────────────────┐
────────→ │  执行拦截器 preHandle()   │
          └────────┬────────────────┘
                   ↓
Step 5    ┌─────────────────────────┐
────────→ │    getHandlerAdapter()   │
          │    获取 HandlerAdapter   │
          └────────┬────────────────┘
                   ↓
Step 6    ┌─────────────────────────┐
────────→ │  ha.handle()           │
          │  执行 Controller 方法    │
          │  返回 ModelAndView       │
          └────────┬────────────────┘
                   ↓
Step 7    ┌─────────────────────────┐
────────→ │  执行拦截器 postHandle() │
          └────────┬────────────────┘
                   ↓
Step 8    ┌─────────────────────────┐
────────→ │  processDispatchResult()│
          │  处理执行结果             │
          └────────┬────────────────┘
                   ↓
Step 9    ┌─────────────────────────┐
────────→ │  resolveViewName()      │
          │  调用 ViewResolver       │
          │  解析视图                 │
          └────────┬────────────────┘
                   ↓
Step 10   ┌─────────────────────────┐
────────→ │  render() 渲染视图       │
          │  生成 HTML/JSON/XML     │
          └────────┬────────────────┘
                   ↓
Step 11   ┌─────────────────────────┐
────────→ │  执行拦截器 afterCompletion()│
          └────────┬────────────────┘
                   ↓
          ┌─────────────────┐
          │  HTTP Response  │
          └─────────────────┘
```

### 详细步骤说明

#### Step 1: 接收 HTTP 请求

```java
// 客户端发送请求
GET /api/users/1 HTTP/1.1
Host: localhost:8080
Accept: application/json
Authorization: Bearer token123
```

#### Step 2: 进入 DispatcherServlet

```java
// DispatcherServlet.java 核心方法
@Override
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) 
        throws Exception {
    
    HttpServletRequest processedRequest = request;
    HandlerExecutionChain mappedHandler = null;
    boolean multipartRequestParsed = false;

    try {
        ModelAndView mv = null;
        Exception dispatchException = null;

        try {
            // 1. 处理文件上传
            processedRequest = checkMultipart(request);
            multipartRequestParsed = (processedRequest != request);

            // 2. 获取处理器链（包含拦截器）
            mappedHandler = getHandler(processedRequest);
            if (mappedHandler == null) {
                noHandlerFound(processedRequest, response);
                return;
            }

            // 3. 获取处理器适配器
            HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

            // 4. 执行拦截器 preHandle
            if (!mappedHandler.applyPreHandle(processedRequest, response)) {
                return; // 拦截器返回 false，流程终止
            }

            // 5. 实际调用 Controller 方法
            mv = ha.handle(processedRequest, response, 
                          mappedHandler.getHandler());

            // 6. 执行拦截器 postHandle
            mappedHandler.applyPostHandle(processedRequest, response, mv);
        }
        catch (Exception ex) {
            dispatchException = ex;
        }
        
        // 7. 处理结果（视图渲染或异常处理）
        processDispatchResult(processedRequest, response, 
                            mappedHandler, mv, dispatchException);
    }
    finally {
        // 8. 清理资源
        if (multipartRequestParsed) {
            cleanupMultipart(processedRequest);
        }
    }
}
```

#### Step 3-4: HandlerMapping & 拦截器

```java
// HandlerExecutionChain 结构
public class HandlerExecutionChain {
    private final Object handler;           // 处理器（Controller 方法）
    private HandlerInterceptor[] interceptors;  // 拦截器数组
    
    // 执行 preHandle
    boolean applyPreHandle(HttpServletRequest request, 
                          HttpServletResponse response) throws Exception {
        for (int i = 0; i < interceptors.length; i++) {
            HandlerInterceptor interceptor = interceptors[i];
            if (!interceptor.preHandle(request, response, this.handler)) {
                // 返回 false，立即触发 afterCompletion
                triggerAfterCompletion(request, response, null);
                return false;
            }
        }
        return true;
    }
}
```

#### Step 5-6: HandlerAdapter 调用 Controller

```java
// RequestMappingHandlerAdapter.java
@Override
protected ModelAndView handleInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    HandlerMethod handlerMethod) throws Exception {
    
    // 1. 准备方法参数（解析 @RequestParam, @PathVariable 等）
    Object[] args = getMethodArgumentValues(request, handlerMethod);
    
    // 2. 调用 Controller 方法（反射）
    Object returnValue = invokeHandlerMethod(request, response, handlerMethod);
    
    // 3. 处理返回值（@ResponseBody, ModelAndView 等）
    return getModelAndView(returnValue, handlerMethod);
}

// 实际反射调用
protected Object invokeHandlerMethod(HttpServletRequest request,
                                   HttpServletResponse response,
                                   HandlerMethod handlerMethod) throws Exception {
    
    // 获取方法对象
    Method method = handlerMethod.getMethod();
    Object bean = handlerMethod.getBean();
    
    // 反射调用
    return method.invoke(bean, args);
}
```

#### Step 7-8: 处理返回值

```java
// 处理返回值的策略
public class RequestResponseBodyMethodProcessor implements HandlerMethodReturnValueHandler {
    
    @Override
    public void handleReturnValue(Object returnValue, 
                                  MethodParameter returnType,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest) throws Exception {
        
        // 1. 标记请求已处理（无需视图渲染）
        mavContainer.setRequestHandled(true);
        
        // 2. 使用 HttpMessageConverter 转换数据
        //    JSON: MappingJackson2HttpMessageConverter
        //    XML:  MappingJackson2XmlHttpMessageConverter
        
        writeWithMessageConverters(returnValue, returnType, webRequest);
    }
}
```

#### Step 9-10: 视图解析与渲染

```java
// processDispatchResult 处理结果
private void processDispatchResult(HttpServletRequest request,
                                  HttpServletResponse response,
                                  HandlerExecutionChain mappedHandler,
                                  ModelAndView mv, Exception exception) throws Exception {
    
    // 1. 异常处理
    if (exception != null) {
        Object handler = mappedHandler != null ? mappedHandler.getHandler() : null;
        mv = processHandlerException(request, response, handler, exception);
    }
    
    // 2. 渲染视图（如果是 ModelAndView 返回）
    if (mv != null && !mv.wasCleared()) {
        render(mv, request, response);
    }
}

// 视图渲染
protected void render(ModelAndView mv, HttpServletRequest request,
                     HttpServletResponse response) throws Exception {
    
    View view;
    String viewName = mv.getViewName();
    
    if (viewName != null) {
        // 解析视图名称为 View 对象
        view = resolveViewName(viewName, mv.getModelInternal(), locale, request);
    }
    
    // 渲染视图（生成 HTML）
    view.render(mv.getModelInternal(), request, response);
}
```

#### Step 11: 拦截器清理

```java
// 无论成功与否，最终都会执行
void triggerAfterCompletion(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Exception ex) throws Exception {
    
    for (int i = this.interceptorIndex; i >= 0; i--) {
        HandlerInterceptor interceptor = this.interceptors[i];
        try {
            interceptor.afterCompletion(request, response, this.handler, ex);
        }
        catch (Throwable ex2) {
            // 记录日志但继续执行其他拦截器的清理
            logger.error("afterCompletion threw exception", ex2);
        }
    }
}
```

---

## 源码分析

### 1. DispatcherServlet 继承结构

```java
// 继承层次
java.lang.Object
    └─ javax.servlet.GenericServlet
        └─ javax.servlet.http.HttpServlet
            └─ org.springframework.web.servlet.HttpServletBean
                └─ org.springframework.web.servlet.FrameworkServlet
                    └─ org.springframework.web.servlet.DispatcherServlet  ← 核心

// FrameworkServlet 职责
// - 初始化 WebApplicationContext
// - 处理 GET/POST/PUT/DELETE 等请求，转发到 doService()

// DispatcherServlet 职责
// - 初始化策略组件（HandlerMapping等）
// - 执行请求分发逻辑（doDispatch）
```

### 2. 初始化过程

```java
// DispatcherServlet.initStrategies() 初始化9大组件
protected void initStrategies(ApplicationContext context) {
    initMultipartResolver(context);      // 文件上传解析器
    initLocaleResolver(context);         // 本地化解析器
    initThemeResolver(context);          // 主题解析器
    initHandlerMappings(context);        // 处理器映射器
    initHandlerAdapters(context);        // 处理器适配器
    initHandlerExceptionResolvers(context); // 异常解析器
    initRequestToViewNameTranslator(context); // 视图名转换器
    initViewResolvers(context);          // 视图解析器
    initFlashMapManager(context);        // Flash属性管理器
}
```

### 3. HandlerMapping 查找过程

```java
// DispatcherServlet.getHandler()
protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
    // 遍历所有 HandlerMapping
    for (HandlerMapping hm : this.handlerMappings) {
        
        // 调用 HandlerMapping.getHandler()
        HandlerExecutionChain handler = hm.getHandler(request);
        
        if (handler != null) {
            return handler; // 找到匹配的处理链
        }
    }
    return null;
}
```

---

## 常用注解映射

### 1. URL 映射注解

```java
@RestController
@RequestMapping("/api/users")  // 类级别路径前缀
public class UserController {

    // GET /api/users
    @GetMapping
    public List<User> list() { }

    // GET /api/users/123
    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) { }

    // POST /api/users
    @PostMapping
    public User create(@RequestBody UserDTO dto) { }

    // PUT /api/users/123
    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody UserDTO dto) { }

    // DELETE /api/users/123
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { }

    // 模糊查询 GET /api/users/search?name=xxx
    @GetMapping("/search")
    public List<User> search(@RequestParam String name) { }
}
```

### 2. 参数绑定注解

| 注解 | 用途 | 示例 |
|------|------|------|
| `@PathVariable` | URL 路径参数 | `/users/{id}` → `@PathVariable Long id` |
| `@RequestParam` | 查询参数 | `?name=xxx` → `@RequestParam String name` |
| `@RequestBody` | 请求体（JSON） | `@RequestBody UserDTO dto` |
| `@RequestHeader` | 请求头 | `@RequestHeader("Authorization") String token` |
| `@CookieValue` | Cookie值 | `@CookieValue("sessionId") String sessionId` |
| `@ModelAttribute` | 表单数据绑定 | `@ModelAttribute User user` |

---

## 异常处理流程

### 1. 异常处理机制

```
Controller 抛出异常
    ↓
HandlerExceptionResolver 处理
    ├── @ExceptionHandler 方法（优先级最高）
    ├── @ControllerAdvice 全局处理
    └── 默认异常处理器
    ↓
返回 Error 视图或 JSON
```

### 2. 全局异常处理

```java
@RestControllerAdvice  // = @ControllerAdvice + @ResponseBody
public class GlobalExceptionHandler {

    // 处理特定异常
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex, WebRequest request) {
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // 处理所有异常
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "服务器内部错误",
            request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 参数校验失败
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        return ResponseEntity.badRequest()
            .body(Map.of("errors", errors));
    }
}
```

---

## 拦截器机制

### 1. 拦截器接口

```java
public interface HandlerInterceptor {
    
    // 在 Controller 方法执行前调用
    // 返回 false 则终止后续流程
    default boolean preHandle(HttpServletRequest request, 
                             HttpServletResponse response,
                             Object handler) throws Exception {
        return true;
    }
    
    // 在 Controller 方法执行后，视图渲染前调用
    default void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           @Nullable ModelAndView modelAndView) throws Exception {
    }
    
    // 在整个请求完成后调用（相当于 finally）
    default void afterCompletion(HttpServletRequest request,
                                  HttpServletResponse response,
                                  Object handler,
                                  @Nullable Exception ex) throws Exception {
    }
}
```

### 2. 自定义拦截器

```java
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response,
                            Object handler) throws Exception {
        
        String token = request.getHeader("Authorization");
        
        if (token == null || !token.startsWith("Bearer ")) {
            log.warn("请求未认证: {}", request.getRequestURI());
            
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未认证\"}");
            
            return false; // 拦截请求
        }
        
        // 解析Token，设置用户上下文
        UserContext.setCurrentUser(parseToken(token));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                 HttpServletResponse response,
                                 Object handler, Exception ex) throws Exception {
        // 清理线程本地变量，防止内存泄漏
        UserContext.clear();
    }
}
```

### 3. 注册拦截器

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;
    
    @Autowired
    private LogInterceptor logInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        
        // 认证拦截器
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")           // 拦截路径
                .excludePathPatterns(                  // 排除路径
                    "/api/login",
                    "/api/register",
                    "/api/public/**"
                );
        
        // 日志拦截器
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/**");
    }
    
    // 其他配置...
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
```

### 4. 拦截器 vs 过滤器

| 特性 | Filter（过滤器） | Interceptor（拦截器） |
|------|----------------|---------------------|
| **所属规范** | Servlet 规范 | Spring MVC |
| **执行时机** | DispatcherServlet 之前 | DispatcherServlet 之后，Controller 之前 |
| **获取 Bean** | 不能直接获取 | 可以注入 Spring Bean |
| **粒度** | URL 级别 | 方法级别（HandlerMethod） |
| **使用场景** | 编码、鉴权、跨域处理 | 权限校验、日志、性能监控 |

```
请求流程：

客户端
   ↓
Servlet Container
   ↓
Filter1 → Filter2 → Filter3    [Servlet Filter Chain]
   ↓
DispatcherServlet
   ↓
Interceptor1.preHandle()         [Spring Interceptor]
   ↓
Controller 执行
   ↓
Interceptor1.postHandle()
   ↓
View 渲染
   ↓
Interceptor1.afterCompletion()
   ↓
响应
```

---

## 最佳实践

### 1. RESTful API 设计

```java
@RestController
@RequestMapping("/api/v1")
public class UserController {

    @Autowired
    private UserService userService;

    // ✅ 使用复数名词
    @GetMapping("/users")
    public PageResult<User> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userService.findPage(page, size);
    }

    // ✅ URL 使用名词，HTTP 方法表示动作
    @GetMapping("/users/{id}")
    public User getById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PostMapping("/users")
    public ResponseEntity<User> create(@RequestBody @Valid UserDTO dto) {
        User user = userService.create(dto);
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(user.getId())
            .toUri();
        
        return ResponseEntity.created(location).body(user);
    }

    @PutMapping("/users/{id}")
    public User update(@PathVariable Long id, @RequestBody @Valid UserDTO dto) {
        return userService.update(id, dto);
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
```

### 2. 统一响应封装

```java
@Data
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
}

// 全局包装器
@RestControllerAdvice(basePackages = "com.example.controller")
public class ResponseWrapper implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                          Class<? extends HttpMessageConverter<?>> converterType) {
        // 所有 Controller 方法返回值都包装
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        
        // 如果已经是 ApiResponse 类型，不再包装
        if (body instanceof ApiResponse) {
            return body;
        }
        // String 类型需要特殊处理
        if (body instanceof String) {
            return body;
        }
        return ApiResponse.success(body);
    }
}
```

### 3. 请求参数校验

```java
@Data
public class UserDTO {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度2-20字符")
    private String username;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Min(value = 18, message = "年龄必须成年")
    @Max(value = 120, message = "年龄输入有误")
    private Integer age;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}

@PostMapping("/users")
public User create(@RequestBody @Valid UserDTO dto) {
    // @Valid 触发校验，失败会抛 MethodArgumentNotValidException
    return userService.create(dto);
}
```

### 4. 性能优化建议

```java
// 1. 开启响应压缩
@Configuration
public class WebConfig {
    
    @Bean
    public FilterRegistrationBean<GzipFilter> gzipFilter() {
        FilterRegistrationBean<GzipFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new GzipFilter());
        registration.addUrlPatterns("/*");
        return registration;
    }
}

// 2. 配置静态资源缓存
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/")
            .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS));
}

// 3. 异步处理
@GetMapping("/reports")
public CompletableFuture<ResponseEntity<?>> generateReport() {
    return CompletableFuture.supplyAsync(() -> {
        // 耗时操作
        Report report = reportService.generate();
        return ResponseEntity.ok(report);
    });
}

// 4. 使用缓存
@Cacheable(value = "users", key = "#id")
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) {
    return userService.findById(id);
}
```

---

## 总结

### 核心流程记忆口诀

> **"一核二找三适配，四拦五执六返回，七视图八渲染九清理"**

1. **核心**：DispatcherServlet 接收请求
2. **找**：HandlerMapping 找处理器
3. **适配**：HandlerAdapter 适配调用
4. **拦**：拦截器 preHandle
5. **执**：执行 Controller 方法
6. **返回**：返回 ModelAndView
7. **视图**：ViewResolver 解析视图
8. **渲染**：渲染视图输出
9. **清理**：拦截器 afterCompletion

### 面试金句

> "Spring MVC 采用前端控制器模式，所有请求都经过 DispatcherServlet 统一处理。核心流程包括：HandlerMapping 查找处理器、HandlerAdapter 适配调用、Controller 执行业务、ViewResolver 解析视图。整个过程通过 HandlerExecutionChain 管理拦截器的织入，支持灵活的扩展机制。"

---

**参考文档**：
- Spring MVC 官方文档：https://docs.spring.io/spring-framework/docs/current/reference/html/web.html
- DispatcherServlet 源码：https://github.com/spring-projects/spring-framework/tree/main/spring-webmvc