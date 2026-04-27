import java.io.*;
import java.lang.reflect.Method;

/**
 * 类加载过程与双亲委派模型演示
 *
 * 核心知识点：
 * 1. 类加载过程：加载 → 验证 → 准备 → 解析 → 初始化
 * 2. 双亲委派模型：Bootstrap -> Extension -> Application -> Custom
 * 3. 为什么要双亲委派：安全性、避免重复加载
 * 4. 如何打破双亲委派：SPI、OSGi、Tomcat
 *
 * @author Java面试宝典
 */
public class ClassLoaderDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("类加载过程与双亲委派模型演示");
        System.out.println("========================================\n");

        // 1. 演示类加载器层次结构
        demonstrateClassLoaderHierarchy();

        // 2. 演示类加载过程
        demonstrateLoadingProcess();

        // 3. 演示双亲委派模型
        demonstrateParentDelegation();

        // 4. 自定义类加载器
        demonstrateCustomClassLoader();

        // 5. 打破双亲委派的场景
        demonstrateSPIPattern();

        System.out.println("\n========================================");
        System.out.println("演示结束！");
        System.out.println("========================================");
    }

    /**
     * 演示类加载器层次结构
     */
    private static void demonstrateClassLoaderHierarchy() {
        System.out.println("【演示1】类加载器层次结构");
        System.out.println("----------------------------------------");

        // 获取系统类加载器（Application ClassLoader）
        ClassLoader appClassLoader = ClassLoader.getSystemClassLoader();
        System.out.println("系统类加载器（AppClassLoader）: " + appClassLoader);

        // 获取扩展类加载器（Extension ClassLoader - JDK9+ 为 Platform ClassLoader）
        ClassLoader extClassLoader = appClassLoader.getParent();
        System.out.println(
            "扩展类加载器（PlatformClassLoader）: " + extClassLoader
        );

        // 获取启动类加载器（Bootstrap ClassLoader）- 返回 null 因为是 C++ 实现
        ClassLoader bootstrapClassLoader = extClassLoader.getParent();
        System.out.println(
            "启动类加载器（BootstrapClassLoader）: " + bootstrapClassLoader
        );

        System.out.println("\n【类加载器 Delegation Chain】");
        System.out.println(
            "Bootstrap ClassLoader <- Platform ClassLoader <- App ClassLoader <- Custom ClassLoader"
        );
        System.out.println(
            "   (null)                (PlatformClassLoader)      (AppClassLoader)      (MyClassLoader)"
        );

        System.out.println("\n【加载路径】");
        System.out.println(
            "Bootstrap:   jre/lib/rt.jar (JDK8) 或 jmods (JDK9+)"
        );
        System.out.println(
            "Platform:  jre/lib/ext/*.jar (JDK8) 或系统类 (JDK9+)"
        );
        System.out.println("App:        classpath");
        System.out.println("Custom:    自定义路径");

        // 加载类的来源
        System.out.println("\n【常用类的加载器】");
        System.out.println(
            "String.class.getClassLoader(): " + String.class.getClassLoader()
        ); // null - Bootstrap
        System.out.println(
            "ClassLoaderDemo.class.getClassLoader(): " +
                ClassLoaderDemo.class.getClassLoader()
        ); // App
        System.out.println(
            "Parent.class.getClassLoader(): " + Parent.class.getClassLoader()
        ); // App

        System.out.println();
    }

    /**
     * 演示类加载过程（5个阶段）
     */
    private static void demonstrateLoadingProcess() {
        System.out.println("【演示2】类加载的5个阶段");
        System.out.println("----------------------------------------");

        System.out.println(
            "类加载完整生命周期：Loading -> Verification -> Preparation -> Resolution -> Initialization"
        );
        System.out.println();

        System.out.println("【阶段1: 加载（Loading）】");
        System.out.println("- 通过全限定名获取二进制字节流");
        System.out.println("- 将字节流转换为方法区的运行时数据结构");
        System.out.println("- 在堆中生成 Class 对象，作为方法区数据的访问入口");
        System.out.println(
            "- 字节流来源：本地文件、网络、数据库、动态代理生成等"
        );
        System.out.println();

        System.out.println("【阶段2: 验证（Verification）】");
        System.out.println("- 文件格式验证：魔数、版本号等");
        System.out.println("- 元数据验证：语义、继承关系等");
        System.out.println("- 字节码验证：数据流和控制流分析");
        System.out.println("- 符号引用验证：能否被正确解析");
        System.out.println("- 目的：保证不会危害 JVM 安全");
        System.out.println();

        System.out.println("【阶段3: 准备（Preparation）】");
        System.out.println(
            "- 为类变量（static）分配内存，设置零值（0、null、false）"
        );
        System.out.println(
            "- 例如：static int a = 123; 在此阶段 a = 0，不是 123"
        );
        System.out.println(
            "- 如果是 final static 常量，在此阶段直接赋值为 123（编译期确定）"
        );
        System.out.println();

        System.out.println("【阶段4: 解析（Resolution）】");
        System.out.println("- 将常量池中的符号引用替换为直接引用");
        System.out.println("  符号引用：java/lang/Object");
        System.out.println("  直接引用：内存中的实际指针地址");
        System.out.println("- 类、接口、字段、方法、方法的符号引用");
        System.out.println("- 可以发生在初始化之后（动态绑定、多态）");
        System.out.println();

        System.out.println("【阶段5: 初始化（Initialization）】");
        System.out.println(
            "- 执行 <clinit>() 方法（静态变量赋值 + 静态代码块）"
        );
        System.out.println("- 按源码中出现的顺序执行");
        System.out.println("- 父类先于子类初始化");
        System.out.println("- 线程安全（加锁）");

        // 触发初始化
        System.out.println("\n触发 Parent 类初始化：");
        new Parent();
        System.out.println("Parent static value = " + Parent.staticValue);

        System.out.println();
    }

    /**
     * 演示双亲委派模型
     */
    private static void demonstrateParentDelegation() {
        System.out.println("【演示3】双亲委派模型（Parent Delegation Model）");
        System.out.println("----------------------------------------");

        System.out.println("【委派流程】");
        System.out.println("1. 类加载器收到加载请求");
        System.out.println("2. 不立即加载，而是委派给父类加载器");
        System.out.println("3. 父类加载器继续向上委派，直到 Bootstrap");
        System.out.println("4. Bootstrap 尝试加载，能否找到类？");
        System.out.println("   - 能加载：成功返回");
        System.out.println("   - 不能加载：向下交给子加载器尝试");
        System.out.println("5. 最终找不到则抛出 ClassNotFoundException");
        System.out.println();

        System.out.println("【源码分析 ClassLoader.loadClass()】");
        System.out.println("// 伪代码");
        System.out.println(
            "protected Class<?> loadClass(String name, boolean resolve) {"
        );
        System.out.println("    // 1. 检查是否已被加载");
        System.out.println("    Class<?> c = findLoadedClass(name);");
        System.out.println("    if (c == null) {");
        System.out.println("        // 2. 委派父类加载器");
        System.out.println("        if (parent != null) {");
        System.out.println("            c = parent.loadClass(name, resolve);");
        System.out.println("        } else {");
        System.out.println("            c = findBootstrapClassOrNull(name);");
        System.out.println("        }");
        System.out.println("        // 3. 父类不能加载，自己尝试");
        System.out.println("        if (c == null) {");
        System.out.println("            c = findClass(name); // 子类实现");
        System.out.println("        }");
        System.out.println("    }");
        System.out.println("    if (resolve) resolveClass(c);");
        System.out.println("    return c;");
        System.out.println("}");
        System.out.println();

        System.out.println("【演示验证】");
        try {
            // 加载 String 类 - 应该由 Bootstrap 加载
            Class<?> stringClass = Class.forName("java.lang.String");
            System.out.println(
                "String 类加载器: " + stringClass.getClassLoader()
            );

            // 加载我们自定义的类 - 应该由 AppClassLoader 加载
            Class<?> customClass = Class.forName("code.Parent");
            System.out.println(
                "Parent 类加载器: " + customClass.getClassLoader()
            );
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("\n【为什么要双亲委派？】");
        System.out.println("1. 安全性（Security）");
        System.out.println("   - 防止核心类库被篡改");
        System.out.println("   - 自定义的 java.lang.String 不会覆盖系统类");
        System.out.println("   - 保证核心 API 的安全性");

        System.out.println("\n2. 避免重复加载（Avoid Duplication）");
        System.out.println("   - 先委托父类加载，确保先查找已加载的类");
        System.out.println("   - 保证类加载的唯一性");

        System.out.println("\n3. 保证扩展性");
        System.out.println("   - 父类加载器对子类加载器是透明的");
        System.out.println("   - 子类通过父类加载系统类");

        System.out.println();
    }

    /**
     * 演示自定义类加载器
     */
    private static void demonstrateCustomClassLoader() {
        System.out.println("【演示4】自定义类加载器");
        System.out.println("----------------------------------------");

        try {
            // 创建自定义类加载器
            MyClassLoader myLoader = new MyClassLoader("自定义类加载器");

            // 加载类
            Class<?> clazz = myLoader.loadClass("code.Parent");
            System.out.println(
                "由自定义类加载器加载: " + clazz.getClassLoader()
            );

            // 创建实例
            Object instance = clazz.getDeclaredConstructor().newInstance();
            Method method = clazz.getMethod("sayHello");
            method.invoke(instance);

            // 验证是否为同一类
            System.out.println("\n【类加载器隔离性】");
            System.out.println(
                "系统加载的 == 自定义加载的: " + (Parent.class == clazz)
            ); // false ！不同的 Class 对象

            System.out.println(
                "\n说明：同一个类被不同类加载器加载，在 JVM 中是不同的类"
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println();
    }

    /**
     * 演示打破双亲委派（SPI模式）
     */
    private static void demonstrateSPIPattern() {
        System.out.println("【演示5】打破双亲委派 - SPI 模式");
        System.out.println("----------------------------------------");

        System.out.println("【SPI（Service Provider Interface）】");
        System.out.println("- JDK 提供接口，厂商实现");
        System.out.println("- 例如：JDBC、JNDI、JAXP");
        System.out.println(
            "- Bootstrap ClassLoader 加载的类需要加载 App ClassLoader 的类"
        );
        System.out.println();

        System.out.println("【问题】");
        System.out.println("- DriverManager 由 Bootstrap 加载");
        System.out.println("- MySQL Driver 由 App ClassLoader 加载");
        System.out.println("- Bootstrap 无法直接访问 App 的类（双亲委派限制）");
        System.out.println();

        System.out.println("【解决方案：Thread Context ClassLoader】");
        System.out.println(
            "Thread.currentThread().setContextClassLoader(appClassLoader);"
        );
        System.out.println(
            "ServiceLoader 使用当前线程的 ContextClassLoader 加载实现类"
        );
        System.out.println();

        System.out.println("【源码示例】java.sql.DriverManager");
        System.out.println(
            "// DriverManager 里通过 ContextClassLoader 加载驱动"
        );
        System.out.println("Class.forName(\"com.mysql.cj.jdbc.Driver\", true,");
        System.out.println(
            "    Thread.currentThread().getContextClassLoader());"
        );
        System.out.println();

        System.out.println("【其他打破双亲委派的场景】");
        System.out.println("1. Tomcat：为每个 Web 应用隔离类加载");
        System.out.println("   - Web 应用加载自己的 Servlet 类");
        System.out.println("   - 优先加载 WEB-INF/classes 下的类");

        System.out.println("\n2. OSGi：模块化热部署");
        System.out.println("   - 每个 Bundle 有自己的类加载器");
        System.out.println("   - 复杂的类加载网络");

        System.out.println("\n3. HotSwap/Spring Boot DevTools");
        System.out.println("   - 运行时热替换类");
        System.out.println("   - 丢弃旧类加载器，新建加载器");
    }

    /**
     * 自定义类加载器
     */
    static class MyClassLoader extends ClassLoader {

        private String name;

        public MyClassLoader(String name) {
            this.name = name;
        }

        @Override
        protected Class<?> findClass(String name)
            throws ClassNotFoundException {
            System.out.println("  MyClassLoader.findClass() 被调用: " + name);

            // 这里应该从自定义路径加载，为了演示简化
            // 实际项目中：读取 .class 文件字节码 -> defineClass()

            // 这里直接委托给父类，演示用
            return super.findClass(name);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
            // 打破双亲委派：自定义类加载器优先自己加载
            // 但 java.lang.* 等核心包仍需委派，否则安全风险

            if (name.startsWith("java.")) {
                // 核心类仍走双亲委派
                return super.loadClass(name, resolve);
            }

            System.out.println("  MyClassLoader.loadClass() 尝试加载: " + name);

            // 先检查是否已加载
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                // 自己尝试加载
                try {
                    c = findClass(name);
                } catch (ClassNotFoundException e) {
                    // 自己加载不了，再委派父类
                    c = super.loadClass(name, resolve);
                }
            }

            if (resolve) {
                resolveClass(c);
            }
            return c;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * 用于演示的类
     */
    static class Parent {

        // 验证准备阶段：static value 初始为 0
        // 初始化阶段：赋值为 10
        static int staticValue = 10;

        // final 常量，编译期确定，准备阶段直接赋值
        static final int CONST_VALUE = 100;

        static {
            System.out.println(
                "  Parent 类 <clinit>() 执行，staticValue = " + staticValue
            );
            staticValue = 20; // 可以修改
        }

        public Parent() {
            System.out.println("  Parent 构造函数执行");
        }

        public void sayHello() {
            System.out.println("  Hello from Parent!");
        }
    }
}

/**
 * 类加载核心知识点总结
 *
 * 【类加载的5个阶段】
 *
 * 1. 加载（Loading）
 *    - 通过类的全限定名获取二进制字节流
 *    - 将字节流转化为方法区的运行时数据结构
 *    - 生成 java.lang.Class 对象
 *    - 类来源：本地文件、网络、jar、动态代理生成（Proxy）、JSP编译等
 *
 * 2. 验证（Verification）
 *    - 文件格式验证：魔数0xCAFEBABE、版本号等
 *    - 元数据验证：类语义、继承关系、final类不被继承等
 *    - 字节码验证：控制流分析、数据流分析
 *    - 符号引用验证：能否找到对应的类、方法、字段
 *    - -Xverify:none 可以关闭（不建议生产环境）
 *
 * 3. 准备（Preparation）
 *    - 为类变量（static）分配内存并设置零值
 *    - static int a = 123; 此阶段 a = 0
 *    - static final int b = 123; 此阶段 b = 123（编译时常量）
 *
 * 4. 解析（Resolution）
 *    - 将常量池的符号引用转为直接引用
 *    - 类、接口、字段、方法解析
 *    - 可以延迟到初始化后（动态绑定）
 *
 * 5. 初始化（Initialization）
 *    - 执行 <clinit>() 方法
 *    - 静态变量赋值 + 静态代码块，按源码顺序执行
 *    - 父类先于子类初始化
 *    - 线程安全（JVM保证同一时间只有一个线程执行 <clinit>）
 *    - 触发条件：
 *      - new/getstatic/putstatic/invokestatic
 *      - 反射调用
 *      - 父类初始化时子类也初始化
 *      - 包含 main 方法的类
 *      - JDK8: default 接口方法所在接口
 *
 * 【双亲委派模型】
 *
 * 层次（JDK8）：
 *   Bootstrap ClassLoader (C++实现，返回null)
 *       |
 *   Extension ClassLoader (jre/lib/ext)
 *       |
 *   Application ClassLoader (CLASSPATH)
 *       |
 *   User Custom ClassLoader
 *
 * JDK9+ 模块系统后：
 *   - Extension 改为 Platform ClassLoader
 *   - 引入层（Layer）概念
 *
 * 双亲委派流程：
 *   1. 检查是否已加载（findLoadedClass）
 *   2. 父类不为空 → 委派父类加载
 *   3. 父类为空 → 委派 Bootstrap
 *   4. 父类加载失败 → 自己加载（findClass）
 *
 * 优点：
 *   - 安全性：防止核心类被篡改，自定义 java.lang.String 无效
 *   - 唯一性：一个类不会被重复加载
 *   - 可见性：子加载器能访问父加载器加载的类
 *
 * 【打破双亲委派】
 *
 * 1. SPI 机制 (Service Provider Interface)
 *    - DriverManager、ServiceLoader
 *    - 使用 Thread Context ClassLoader (TCCL)
 *    - 父类加载器委托子类加载器加载类
 *
 *    Thread.currentThread().setContextClassLoader(appClassLoader);
 *
 * 2. Tomcat Web 容器
 *    - 为每个 WebApp 提供隔离
 *    - Common、Shared、Catalina、WebApp 多层类加载器
 *    - WebApp 优先加载自己的类
 *
 * 3. OSGi 模块化
 *    - 每个 Bundle 独立类加载器
 *    - 复杂的类加载网络
 *    - 热部署支持
 *
 * 【常见问题】
 *
 * Q: 能否自定义 java.lang.String？
 * A: 不能。双亲委派保证 String 由 Bootstrap 加载。
 *    如果自定义类加载器强制 loadClass 不委派，可以加载，
 *    但无法使用，因为 JVM 安全机制会抛出 SecurityException。
 *
 * Q: 类加载器是什么？
 * A: 类加载器也是类（ClassLoader 子类），但 Bootstrap 是 C++ 实现。
 *
 * Q: 比较两个类是否相等？
 * A: 必须同时满足：类的全限定名相同 + 被同一类加载器加载。
 *    Class.forName() 创建的类 == new MyClassLoader().loadClass() 创建的类 → false
 *
 * Q: 什么时候触发类加载？
 * A: 主动使用的5种情况（见初始化部分），其他如通过子类.父类静态字段不会触发子类初始化。
 *
 * 【面试要点】
 * 1. 能讲清楚5个阶段各自做什么
 * 2. 能画双亲委派模型结构图，讲清楚流程
 * 3. 知道为什么要双亲委派（安全性）
 * 4. 知道如何打破（SPI、Tomcat、OSGi）
 * 5. 了解 JDK9+ 模块系统的变化
 */
