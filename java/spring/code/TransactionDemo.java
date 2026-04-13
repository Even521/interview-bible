package com.example.spring.code;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * Spring 事务管理示例代码
 * 演示各种事务传播行为和事务配置
 */
@Service
public class TransactionDemo {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * 1. 基本事务配置
     * REQUIRED: 如果当前没有事务，就新建一个事务，如果已经存在一个事务中，加入这个事务
     */
    @Transactional
    public void createUserWithTransaction(User user) {
        userRepository.save(user);
        // 模拟业务操作
        doSomething();
    }

    /**
     * 2. 事务传播行为 - REQUIRES_NEW
     * 新建事务，如果当前存在事务，把当前事务挂起
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createOrderRequiresNew(Order order) {
        orderRepository.save(order);
    }

    /**
     * 3. 事务传播行为 - NESTED
     * 如果当前存在事务，则在嵌套事务内执行；如果当前没有事务，则执行 REQUIRED 类似的操作
     */
    @Transactional(propagation = Propagation.NESTED)
    public void updateUserNested(User user) {
        userRepository.save(user);
    }

    /**
     * 4. 事务传播行为 - SUPPORTS
     * 支持当前事务，如果当前没有事务，就以非事务方式执行
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public User getUserSupports(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    /**
     * 5. 事务传播行为 - NOT_SUPPORTED
     * 以非事务方式执行，如果当前存在事务，就把当前事务挂起
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void logOperationNotSupported(String operation) {
        // 日志记录，不需要事务
        System.out.println("Operation: " + operation);
    }

    /**
     * 6. 事务传播行为 - MANDATORY
     * 使用当前事务，如果当前没有事务，就抛出异常
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void mandatoryOperation() {
        // 必须在事务中执行
        System.out.println("Mandatory operation");
    }

    /**
     * 7. 事务传播行为 - NEVER
     * 以非事务方式执行，如果当前存在事务，则抛出异常
     */
    @Transactional(propagation = Propagation.NEVER)
    public void neverWithTransaction() {
        // 不能在事务中执行
        System.out.println("Never with transaction");
    }

    /**
     * 8. 只读事务
     * 优化查询性能，不允许进行写操作
     */
    @Transactional(readOnly = true)
    public User getUserReadOnly(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    /**
     * 9. 指定回滚异常
     * rollbackFor: 遇到什么异常时回滚
     * noRollbackFor: 遇到什么异常时不回滚
     */
    @Transactional(rollbackFor = Exception.class, noRollbackFor = IllegalArgumentException.class)
    public void updateWithRollbackConfig(User user) throws Exception {
        userRepository.save(user);
        if (user.getName() == null) {
            throw new IllegalArgumentException("Name cannot be null"); // 不会回滚
        }
        if (user.getEmail() == null) {
            throw new Exception("Email cannot be null"); // 会回滚
        }
    }

    /**
     * 10. 事务超时设置
     * 单位为秒
     */
    @Transactional(timeout = 30)
    public void longRunningOperation() {
        // 执行时间较长的操作
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 11. 手动控制事务回滚
     * 使用 TransactionAspectSupport 手动设置回滚
     */
    public void manualRollback() {
        try {
            // 执行业务逻辑
            doSomething();
            // 根据某些条件手动回滚
            if (needRollback()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new RuntimeException("Transaction rolled back", e);
        }
    }

    /**
     * 12. 同类方法调用事务失效问题演示
     * 注意：Spring 事务是基于 AOP 代理的，同类方法调用会导致事务失效
     */
    public void outerMethod() {
        // 这里直接调用 innerMethod，事务不会生效！
        // 因为不是通过代理对象调用的
        innerMethod(); // 事务失效！

        // 正确做法：获取代理对象后调用
        // ((TransactionDemo) AopContext.currentProxy()).innerMethod();
    }

    @Transactional
    public void innerMethod() {
        userRepository.save(new User());
    }

    private void doSomething() {
        // 模拟业务操作
    }

    private boolean needRollback() {
        return false;
    }
}

/**
 * 实体类
 */
class User {
    private Long id;
    private String name;
    private String email;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

class Order {
    private Long id;
    private Long userId;
    private String orderNo;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
}

/**
 * 模拟 Repository 接口
 */
interface UserRepository {
    User save(User user);
    java.util.Optional<User> findById(Long id);
}

interface OrderRepository {
    Order save(Order order);
}
