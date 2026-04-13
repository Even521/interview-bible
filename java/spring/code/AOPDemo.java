package com.example.spring.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * Spring AOP 示例代码
 * 演示了各种通知类型和切点表达式
 */
@Configuration
@ComponentScan("com.example.spring.aop")
@EnableAspectJAutoProxy // 开启AOP自动代理
public class AOPDemo {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context =
            new AnnotationConfigApplicationContext(AOPDemo.class);

        UserService userService = context.getBean(UserService.class);

        // 测试正常执行
        System.out.println("=== 测试正常执行 ===");
        userService.addUser("张三");

        System.out.println("\n=== 测试异常执行 ===");
        try {
            userService.deleteUser(-1);
        } catch (Exception e) {
            System.out.println("捕获到异常: " + e.getMessage());
        }

        System.out.println("\n=== 测试返回值 ===");
        String user = userService.findUserById(1);
        System.out.println("返回值: " + user);

        context.close();
    }
}

/**
 * 业务接口
 */
interface UserService {
    void addUser(String username);
    void deleteUser(int id);
    String findUserById(int id);
}

/**
 * 业务实现类
 */
@Service
class UserServiceImpl implements UserService {

    @Override
    public void addUser(String username) {
        System.out.println("执行业务: 添加用户 - " + username);
    }

    @Override
    public void deleteUser(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("用户ID不能为负数");
        }
        System.out.println("执行业务: 删除用户 - ID: " + id);
    }

    @Override
    public String findUserById(int id) {
        System.out.println("执行业务: 查询用户 - ID: " + id);
        return "User-" + id;
    }
}

/**
 * 日志切面
 */
@Component
@Aspect
class LoggingAspect {

    /**
     * 定义切点：匹配UserService的所有方法
     */
    @Pointcut("execution(* com.example.spring.aop.UserService.*(..))")
    public void userServicePointcut() {}

    /**
     * 前置通知：目标方法执行前执行
     */
    @Before("userServicePointcut()")
    public void beforeAdvice() {
        System.out.println("[前置通知] 方法即将执行...");
    }

    /**
     * 后置通知：目标方法无论是否异常都会执行
     */
    @After("userServicePointcut()")
    public void afterAdvice() {
        System.out.println("[后置通知] 方法执行结束...");
    }

    /**
     * 返回通知：目标方法正常返回后执行
     */
    @AfterReturning(pointcut = "userServicePointcut()", returning = "result")
    public void afterReturningAdvice(Object result) {
        System.out.println("[返回通知] 方法返回值: " + result);
    }

    /**
     * 异常通知：目标方法抛出异常后执行
     */
    @AfterThrowing(pointcut = "userServicePointcut()", throwing = "ex")
    public void afterThrowingAdvice(Exception ex) {
        System.out.println("[异常通知] 方法抛出了异常: " + ex.getMessage());
    }

    /**
     * 环绕通知：最强大的通知，可以控制目标方法的执行
     */
    @Around("userServicePointcut()")
    public Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("[环绕通知] 方法调用前...");

        long startTime = System.currentTimeMillis();

        // 执行目标方法
        Object result = joinPoint.proceed();

        long endTime = System.currentTimeMillis();
        System.out.println("[环绕通知] 方法调用后，耗时: " + (endTime - startTime) + "ms");

        return result;
    }
}

/**
 * 性能监控切面（演示多个切面）
 */
@Component
@Aspect
class PerformanceAspect {

    /**
     * 切点：匹配service包下所有类的所有方法
     */
    @Pointcut("execution(* com.example.spring.aop.*Service.*(..))")
    public void serviceLayer() {}

    @Around("serviceLayer()")
    public Object measurePerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long end = System.currentTimeMillis();

        System.out.println("[性能监控] " + className + "." + methodName + "() 执行时间: " + (end - start) + "ms");
        return result;
    }
}

/**
 * 自定义注解切面（演示注解驱动AOP）
 */
@Component
@Aspect
class AnnotationAspect {

    /**
     * 使用自定义注解作为切点
     */
    @Pointcut("@annotation(com.example.spring.aop.LogOperation)")
    public void logOperationPointcut() {}

    @Around("logOperationPointcut()")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("[注解AOP] 记录操作日志: " + joinPoint.getSignature().getName());
        return joinPoint.proceed();
    }
}

/**
 * 自定义注解
 */
@interface LogOperation {
    String value() default "";
}
