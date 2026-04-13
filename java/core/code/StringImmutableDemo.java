package code;

import java.util.HashSet;
import java.util.Set;

/**
 * String 不可变性（Immutable）设计原因演示
 *
 * 核心知识点：
 * 1. String 不可变的本质（final 类 + final char[]/byte[] value）
 * 2. 为什么要设计为不可变（安全性、性能、线程安全）
 * 3. 字符串常量池（String Pool/String Intern Pool）
 * 4. 不可变带来的好处和潜在问题
 *
 * @author Java面试宝典
 */
public class StringImmutableDemo {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("String 不可变性设计原因演示");
        System.out.println("========================================\n");

        // 1. 演示 String 的不可变性
        demonstrateImmutability();

        // 2. 字符串常量池
        demonstrateStringPool();

        // 3. 安全性演示
        demonstrateSecurity();

        // 4. HashCode 缓存
        demonstrateHashCodeCache();

        // 5. 线程安全
        demonstrateThreadSafety();

        // 6. 如果 String 可变会怎样
        demonstrateMutableStringProblem();

        // 7. 替代方案：StringBuilder/StringBuffer
        demonstrateAlternatives();

        System.out.println("\n========================================");
        System.out.println("演示结束！");
        System.out.println("========================================");
    }

    /**
     * 演示 String 的不可变性
     */
    private static void demonstrateImmutability() {
        System.out.println("【演示1】String 的不可变性");
        System.out.println("----------------------------------------");

        String str = "Hello";
        System.out.println("初始 String: \"" + str + "\"");
        System.out.println("hashCode: " + str.hashCode());

        // 尝试"修改"字符串
        String newStr = str.concat(" World");
        System.out.println("\n执行 str.concat(\" World\") 后:");
        System.out.println("原字符串 str: \"" + str + "\"（未改变！）");
        System.out.println("新字符串 newStr: \"" + newStr + "\"（新对象）");

        // 所有"修改"操作都返回新对象
        String upper = str.toUpperCase();
        System.out.println("\n执行 str.toUpperCase() 后:");
        System.out.println("原字符串 str: \"" + str + "\"（未改变！）");
        System.out.println("新字符串 upper: \"" + upper + "\"");

        // 证明是不同对象
        System.out.println("\nstr == newStr: " + (str == newStr));
        System.out.println("str == upper: " + (str == upper));

        System.out.println("\n【核心结论】");
        System.out.println("String 是不可变的：任何\"修改\"操作都返回新对象，原对象不变");
        System.out.println("String 的本质：final class + final byte[] value");
        System.out.println();
    }

    /**
     * 演示字符串常量池
     */
    private static void demonstrateStringPool() {
        System.out.println("【演示2】字符串常量池（String Pool）");
        System.out.println("----------------------------------------");

        // 字面量创建（放入常量池）
        String s1 = "Java";
        String s2 = "Java";

        // new 创建（在堆内存）
        String s3 = new String("Java");

        System.out.println("s1 = \"Java\"（字面量）");
        System.out.println("s2 = \"Java\"（字面量）");
        System.out.println("s3 = new String(\"Java\")");

        // 比较
        System.out.println("\ns1 == s2: " + (s1 == s2) + "（常量池复用）");
        System.out.println("s1 == s3: " + (s1 == s3) + "（堆内存新对象）");
        System.out.println("s1.equals(s3): " + s1.equals(s3) + "（内容相同）");

        // intern() 方法
        String s4 = s3.intern();
        System.out.println("\ns4 = s3.intern(): " + s4);
        System.out.println("s1 == s4: " + (s1 == s4) + "（intern 返回常量池引用）");

        // 常量池的性能优势
        System.out.println("\n【常量池优势】");
        System.out.println("1. 相同字符串只存储一份，节省内存");
        System.out.println("2. hashCode 缓存，提高 HashMap 性能");
        System.out.println("3. 快速比较（== 可以先比较地址）");
        System.out.println();
    }

    /**
     * 演示安全性原因
     */
    private static void demonstrateSecurity() {
        System.out.println("【演示3】安全性原因 - String 作为参数传递");
        System.out.println("----------------------------------------");

        // 模拟数据库连接
        class DatabaseConnector {
            private String hostname;
            private String username;
            private String password;

            public DatabaseConnector(String hostname, String username, String password) {
                this.hostname = hostname;
                this.username = username;
                this.password = password;
            }

            public void connect() {
                System.out.println("连接数据库: " + hostname);
                System.out.println("用户名: " + username);
                System.out.println("密码: " + password.substring(0, Math.min(3, password.length())) + "***");
            }
        }

        // 用户提供的凭据
        String hostname = "localhost";
        String username = "admin";
        String password = "secret123";

        System.out.println("原始密码: " + password);
        System.out.println("密码 hashCode: " + password.hashCode());

        // 创建连接
        DatabaseConnector connector = new DatabaseConnector(hostname, username, password);
        connector.connect();

        // 密码不可变，无法被恶意修改
        System.out.println("\n【安全优势】");
        System.out.println("1. String 不可变，防止被恶意修改（如通过反射）");
        System.out.println("2. 作为网络连接、文件路径等参数时更安全");
        System.out.println("3. 即使传递到不可信的代码，值也不会被篡改");
        System.out.println();
    }

    /**
     * 演示 HashCode 缓存
     */
    private static void demonstrateHashCodeCache() {
        System.out.println("【演示4】HashCode 缓存");
        System.out.println("----------------------------------------");

        String str = "abcdefghijklmnopqrstuvwxyz";
        System.out.println("字符串: \"" + str + "\"");

        // 第一次计算 hashCode
        long start = System.nanoTime();
        int hash1 = str.hashCode();
        long end = System.nanoTime();
        System.out.println("第一次 hashCode(): " + hash1);
        System.out.println("耗时: " + (end - start) + " ns");

        // 第二次获取 hashCode（从缓存读取）
        start = System.nanoTime();
        int hash2 = str.hashCode();
        end = System.nanoTime();
        System.out.println("第二次 hashCode(): " + hash2);
        System.out.println("耗时: " + (end - start) + " ns（从缓存读取，极快）");

        // String 的 hashCode 缓存原理
        System.out.println("\n【String hashCode 源码】");
        System.out.println("public int hashCode() {");
        System.out.println("    int h = hash;  // 缓存字段");
        System.out.println("    if (h == 0 && value.length > 0) {");
        System.out.println("        // 计算并缓存");
        System.out.println("        hash = h = ...");
        System.out.println("    }");
        System.out.println("    return h;");
        System.out.println("}");

        System.out.println("\n【缓存优势】");
        System.out.println("1. String 常用作 HashMap key，频繁计算 hashCode");
        System.out.println("2. 不可变性保证 hashCode 一旦计算就不会改变");
        System.out.println("3. 节省 CPU，提高 HashMap 性能");
        System.out.println();
    }

    /**
     * 演示线程安全
     */
    private static void demonstrateThreadSafety() {
        System.out.println("【演示5】线程安全（天然线程安全）");
        System.out.println("----------------------------------------");

        final String shared = "SharedData";

        // 多个线程"修改"同一个字符串
        Runnable task = () -> {
            for (int i = 0; i < 3; i++) {
                // 这个操作创建新对象，不影响原字符串
                String newStr = shared + "_" + Thread.currentThread().getName() + "_" + i;
                System.out.println(Thread.currentThread().getName() + ": " + newStr);
            }
        };

        // 启动多个线程
        Thread t1 = new Thread(task, "Thread-1");
        Thread t2 = new Thread(task, "Thread-2");

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 原字符串从未改变
        System.out.println("\n原始字符串: \"" + shared + "\"（始终未变）");

        System.out.println("\n【线程安全优势】");
        System.out.println("1. String 天然线程安全，无需同步");
        System.out.println("2. 多线程共享 String 不会有并发问题");
        System.out.println("3. 节省同步开销");
        System.out.println();
    }

    /**
     * 演示如果 String 可变会怎样
     */
    private static void demonstrateMutableStringProblem() {
        System.out.println("【演示6】假设 String 是可变的（演示潜在问题）");
        System.out.println("----------------------------------------");

        // 使用 StringBuilder 模拟可变 String
        StringBuilder mutableKey = new StringBuilder("Key");

        Set<StringBuilder> set = new HashSet<>();
        set.add(mutableKey);

        System.out.println("添加 Key 到 HashSet");
        System.out.println("contains(Key): " + set.contains(mutableKey));
        System.out.println("hashCode: " + mutableKey.hashCode());

        // 修改内容
        mutableKey.append("_Modified");
        System.out.println("\n修改后: " + mutableKey);
        System.out.println("新 hashCode: " + mutableKey.hashCode());

        // 问题：找不到之前的元素了
        System.out.println("\n查找原 Key:");
        System.out.println("contains(Key): " + set.contains(new StringBuilder("Key")) + "（找不到了！）");

        // 更严重的：内存泄漏风险
        System.out.println("\n【如果 String 可变会出现的问题】");
        System.out.println("1. HashMap key 被修改后，hashCode 改变，找不到数据");
        System.out.println("2. 数据库密码被修改，连接被劫持");
        System.out.println("3. 字符串常量池中的值被修改，影响所有引用");
        System.out.println("4. hashCode 缓存失效，每次重新计算");
        System.out.println();
    }

    /**
     * 演示替代方案
     */
    private static void demonstrateAlternatives() {
        System.out.println("【演示7】需要可变字符串时的替代方案");
        System.out.println("----------------------------------------");

        // StringBuilder（单线程，无同步，最快）
        StringBuilder sb = new StringBuilder();
        sb.append("Hello ");
        sb.append("World");
        sb.append("!");
        String result1 = sb.toString();
        System.out.println("StringBuilder: " + result1);

        // StringBuffer（多线程，有同步，较慢）
        StringBuffer sbf = new StringBuffer();
        sbf.append("Hello ");
        sbf.append("World");
        sbf.append("!");
        String result2 = sbf.toString();
        System.out.println("StringBuffer: " + result2);

        // JDK 9+ String.concat 优化
        String s = "Hello ".concat("World").concat("!");
        System.out.println("String.concat: " + s);

        // String.join（JDK 8+）
        String joined = String.join("-", "2024", "01", "15");
        System.out.println("String.join: " + joined);

        // String.format
        String formatted = String.format("Name: %s, Age: %d", "Alice", 25);
        System.out.println("String.format: " + formatted);

        System.out.println("\n【选择建议】");
        System.out.println("1. 单线程字符串拼接：StringBuilder（优先）");
        System.out.println("2. 多线程字符串拼接：StringBuffer");
        System.out.println("3. 常量拼接：直接使用 +（编译器优化）");
        System.out.println("4. 格式化输出：String.format / MessageFormat");
        System.out.println();
    }

    /**
     * 模拟的可变字符串类（用于演示问题）
     */
    static class MutableString {
        private char[] value;

        public MutableString(String str) {
            this.value = str.toCharArray();
        }

        public void setChar(int index, char c) {
            value[index] = c;
        }

        @Override
        public String toString() {
            return new String(value);
        }
    }
}

/**
 * String 不可变性设计原因总结
 *
 * 【什么是不可变】
 * 1. final class String：不能被继承
 * 2. final byte[]/char[] value：数组引用不能改变
 * 3. 不提供修改 value 的方法（没有 setter）
 *
 * 【设计原因 - 五大核心】
 *
 * 1. 安全性（Security）
 *    - String 常用作敏感数据（密码、文件路径、网络地址）
 *    - 不可变性防止数据被恶意修改（如通过反射篡改）
 *    - 作为 Map key 时不会被篡改
 *
 * 2. 性能优化（Performance）
 *    - hashCode 缓存：String.hashCode() 计算一次后缓存
 *    - 字符串常量池：相同字符串只存储一份，节省内存
 *    - 快速比较：== 可以先比较地址，相同则无需 equals
 *
 * 3. 线程安全（Thread Safety）
 *    - 天然线程安全，无需同步
 *    - 多线程共享不会有并发问题
 *    - 节省同步开销
 *
 * 4. 字符串常量池（String Pool）
 *    - 字面量自动放入常量池
 *    - 相同字符串共享引用
 *    - intern() 方法手动入池
 *
 * 5. 类加载安全
 *    - 类名、包名用 String 存储，不可变确保安全
 *    - 防止类加载器被欺骗
 *
 * 【潜在缺点】
 * 1. 频繁拼接产生大量临时对象
 *    - 解决：使用 StringBuilder/StringBuffer
 * 2. 修改操作效率低（必须创建新对象）
 *    - 解决：StringBuilder（可变）
 * 3. 内存占用（字符数组 + 对象头）
 *    - JDK 9+ 优化：使用 byte[]（Latin1/UTF-16）
 *
 * 【JDK 9+ 优化】
 * 1. 内部从 char[] 改为 byte[] + coder
 *    - Latin1（1字节）：纯 ASCII 字符串节省一半内存
 *    - UTF-16（2字节）：包含非 ASCII 字符
 * 2. 字符串拼接优化（invokedynamic + StringConcatFactory）
 *
 * 【最佳实践】
 * 1. 优先使用字面量创建（自动入常量池）
 *    String s = "abc"; // 推荐
 *    String s = new String("abc"); // 不推荐
 * 2. 大量拼接用 StringBuilder
 *    - for 循环内避免使用 +
 * 3. 敏感信息及时清除（虽然 String 不可变，但可以用反射修改）
 * 4. 使用 equals 比较内容，不用 ==
 * 5. 需要修改时考虑 char[]（如密码处理）
 */
