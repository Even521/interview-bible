interview-bible\java\spring\code\BeanLifecycleDemo.java
package com.example.spring.lifecycle;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.ApplicationContextAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Spring Bean 生命周期完整示例
 * 演示Bean从实例化到销毁的完整过程
 */
@Configuration
@ComponentScan("com.example.spring.lifecycle")
public class BeanLifecycleDemo {

    public static void main(String[] args) {
        System.out.println("========== Spring容器启动 ==========\n");

        AnnotationConfigApplicationContext context =
            new AnnotationConfigApplicationContext(BeanLifecycleDemo.class);

        System.out.println("\n========== 从容器中获取Bean ==========");

        // 获取Bean
        LifecycleBean bean = context.getBean("lifecycleBean", LifecycleBean.class);
        bean.doSomething();

        System.out.println("\n========== Spring容器关闭 ==========\n");

        // 关闭容器
        context.close();
    }

    /**
     * 配置另一个Bean用于演示
     */
    @Bean(initMethod = "customInit", destroyMethod = "customDestroy")
    public CustomLifecycleBean customLifecycleBean() {
        return new CustomLifecycleBean();
    }

    /**
     * BeanPostProcessor - 对所有Bean生效
     */
    @Bean
    public CustomBeanPostProcessor customBeanPostProcessor() {
        return new CustomBeanPostProcessor();
    }
}

/**
 * 完整生命周期示例Bean
 * 实现了各种Aware接口和方法回调
 */
@Component
@Scope("singleton") // 默认就是singleton，显式声明便于理解
class LifecycleBean implements InitializingBean, DisposableBean,
    BeanNameAware, BeanFactoryAware, ApplicationContextAware {

    private String name;
    private String beanName;
    private BeanFactory beanFactory;
    private ApplicationContext applicationContext;

    public LifecycleBean() {
        System.out.println("【1】构造器：实例化 LifecycleBean");
    }

    /**
     * 属性赋值注入
     */
    public void setName(String name) {
        this.name = name;
        System.out.println("【2】属性赋值：设置 name = " + name);
    }

    /**
     * BeanNameAware - 获取Bean名称
     */
    @Override
    public void setBeanName(String name) {
        this.beanName = name;
        System.out.println("【3】BeanNameAware.setBeanName()：Bean名称 = " + name);
    }

    /**
     * BeanFactoryAware - 获取BeanFactory
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        System.out.println("【4】BeanFactoryAware.setBeanFactory()：获取到BeanFactory");
    }

    /**
     * ApplicationContextAware - 获取ApplicationContext
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        System.out.println("【5】ApplicationContextAware.setApplicationContext()：获取到ApplicationContext");
    }

    /**
     * @PostConstruct - JSR-250注解
     */
    @PostConstruct
    public void postConstruct() {
        System.out.println("【6】@PostConstruct：注解初始化方法");
    }

    /**
     * InitializingBean - Spring提供的初始化接口
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("【7】InitializingBean.afterPropertiesSet()：属性设置后");
    }

    /**
     * 自定义初始化方法（通过XML或@Bean的initMethod指定）
     */
    public void customInit() {
        System.out.println("【8】自定义init-method：自定义初始化方法");
    }

    /**
     * 业务方法
     */
    public void doSomething() {
        System.out.println("【业务执行】LifecycleBean 正在执行业务逻辑...");
        System.out.println(" - Bean名称: " + beanName);
        System.out.println(" - BeanFactory: " + (beanFactory != null ? "已获取" : "未获取"));
        System.out.println(" - ApplicationContext: " + (applicationContext != null ? "已获取" : "未获取"));
    }

    // ================== 销毁阶段 ==================

    /**
     * @PreDestroy - JSR-250注解
     */
    @PreDestroy
    public void preDestroy() {
        System.out.println("【9】@PreDestroy：注解销毁方法");
    }

    /**
     * DisposableBean - Spring提供的销毁接口
     */
    @Override
    public void destroy() throws Exception {
        System.out.println("【10】DisposableBean.destroy()：销毁方法");
    }

    /**
     * 自定义销毁方法（通过XML或@Bean的destroyMethod指定）
     */
    public void customDestroy() {
        System.out.println("【11】自定义destroy-method：自定义销毁方法");
    }
}

/**
 * 自定义BeanPostProcessor
 * 用于在Bean初始化前后进行额外处理
 */
@Component
class CustomBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof LifecycleBean) {
            System.out.println(" -> BeanPostProcessor.postProcessBeforeInitialization()：初始化前处理 " + beanName);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof LifecycleBean) {
            System.out.println(" -> BeanPostProcessor.postProcessAfterInitialization()：初始化后处理 " + beanName);
        }
        return bean;
    }
}

/**
 * 使用@Bean注解指定的自定义生命周期Bean
 */
class CustomLifecycleBean {

    public CustomLifecycleBean() {
        System.out.println("自定义Bean：实例化 CustomLifecycleBean");
    }

    public void customInit() {
        System.out.println("自定义Bean：执行customInit方法");
    }

    public void customDestroy() {
        System.out.println("自定义Bean：执行customDestroy方法");
    }
}

/**
 * Prototype作用域Bean示例
 * 注意：prototype Bean的销毁不由Spring管理
 */
@Component
@Scope("prototype")
class PrototypeBean {

    public PrototypeBean() {
        System.out.println("PrototypeBean：实例化");
    }

    @PostConstruct
    public void init() {
        System.out.println("PrototypeBean：初始化");
    }

    @PreDestroy
    public void destroy() {
        // 注意：对于prototype作用域，此方法不会被自动调用
        System.out.println("PrototypeBean：销毁（可能不会被调用）");
    }
}

/**
 * 生命周期流程总结说明
 *
 * ============================================
 * 完整的Spring Bean生命周期
 * ============================================
 *
 * 1. 实例化（Instantiation）
 * - 调用构造器创建Bean实例
 *
 * 2. 属性赋值（Populate Properties）
 * - 依赖注入，填充属性值
 *
 * 3. Aware接口回调
 * - setBeanName() - 知晓Bean名称
 * - setBeanFactory() - 获取BeanFactory
 * - setApplicationContext() - 获取ApplicationContext
 *
 * 4. BeanPostProcessor.postProcessBeforeInitialization()
 * - 初始化前的额外处理
 *
 * 5. 初始化方法（多种方式按顺序执行）
 * - @PostConstruct - JSR-250注解
 * - afterPropertiesSet() - InitializingBean接口
 * - 自定义init-method - XML或@Bean配置
 *
 * 6. BeanPostProcessor.postProcessAfterInitialization()
 * - 初始化后的额外处理（如AOP代理在此包装）
 *
 * 7. Bean就绪，可供使用
 *
 * 8. 销毁阶段（容器关闭时）
 * - @PreDestroy - JSR-250注解
 * - destroy() - DisposableBean接口
 * - 自定义destroy-method - XML或@Bean配置
 *
 * ============================================
 *
 * 注意事项：
 * 1. 同类型的回调按上述顺序执行
 * 2. BeanPostProcessor对所有Bean生效
 * 3. Prototype作用域的Bean不会被容器管理销毁
 * 4. 建议使用JSR-250注解（@PostConstruct/@PreDestroy），降低耦合
 * 5. 销毁方法可能在应用非正常终止时不会执行（如kill -9）
 */
