import java.util.*;

/**
 * equals 与 hashCode 契约演示
 *
 * 核心知识点：
 * 1. == vs equals 的区别
 * 2. equals 契约（自反性、对称性、传递性、一致性）
 * 3. hashCode 契约（相等对象必有相同 hashCode，不等对象 hashCode 可以相同）
 * 4. 为什么重写 equals 必须重写 hashCode
 *
 * @author Java面试宝典
 */
public class EqualsHashCodeDemo {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("equals 与 hashCode 契约演示");
        System.out.println("========================================\n");

        // 1. == vs equals 对比
        demonstrateEqualsVsEqual();

        // 2. equals 契约演示
        demonstrateEqualsContract();

        // 3. hashCode 契约演示
        demonstrateHashCodeContract();

        // 4. 为什么重写 equals 必须重写 hashCode
        demonstrateHashCodeViolation();

        // 5. 正确实现示例
        demonstrateCorrectImplementation();

        // 6. 常见错误
        demonstrateCommonMistakes();

        System.out.println("\n========================================");
        System.out.println("演示结束！");
        System.out.println("========================================");
    }

    /**
     * 演示 == vs equals 的区别
     */
    private static void demonstrateEqualsVsEqual() {
        System.out.println("【演示1】== vs equals 的区别");
        System.out.println("----------------------------------------");

        // 1. 基本数据类型：== 比较值
        int a = 10;
        int b = 10;
        System.out.println("1. 基本数据类型 (int):");
        System.out.println("   a == b: " + (a == b) + "  // 比较值");

        // 2. 引用类型：== 比较内存地址
        Person p1 = new Person("Alice", 25);
        Person p2 = new Person("Alice", 25);
        System.out.println("\n2. 引用类型 (Person):");
        System.out.println("   p1 == p2: " + (p1 == p2) + "  // 比较内存地址");
        System.out.println(
            "   p1.equals(p2): " +
                p1.equals(p2) +
                "  // 比较内容（未重写时同==）"
        );

        // 3. String 的坑
        String s1 = "hello";
        String s2 = "hello";
        String s3 = new String("hello");
        System.out.println("\n3. String 的特殊性：");
        System.out.println(
            "   s1 == s2: " + (s1 == s2) + "  // true（字符串常量池）"
        );
        System.out.println(
            "   s1 == s3: " + (s1 == s3) + "  // false（不同对象）"
        );
        System.out.println(
            "   s1.equals(s3): " + s1.equals(s3) + "  // true（内容相同）"
        );

        // 4. Integer 的缓存陷阱
        Integer i1 = 127;
        Integer i2 = 127;
        Integer i3 = 128;
        Integer i4 = 128;
        System.out.println("\n4. Integer 的缓存陷阱：");
        System.out.println(
            "   i1 == i2 (127): " + (i1 == i2) + "  // true（缓存 -128~127）"
        );
        System.out.println(
            "   i3 == i4 (128): " + (i3 == i4) + "  // false（超出缓存范围）"
        );
        System.out.println(
            "   i3.equals(i4): " + i3.equals(i4) + "  // true（内容相等）"
        );

        System.out.println("\n【核心区别总结】");
        System.out.println(" == 比较的是内存地址（基本类型比较值）");
        System.out.println(" equals 默认也是比较内存地址（Object类实现）");
        System.out.println(" String/Integer等类重写了equals，比较的是内容");
        System.out.println();
    }

    /**
     * 演示 equals 契约
     */
    private static void demonstrateEqualsContract() {
        System.out.println("【演示2】equals 的四大契约");
        System.out.println("----------------------------------------");

        PersonWithEquals p1 = new PersonWithEquals("Alice", 25);
        PersonWithEquals p2 = new PersonWithEquals("Alice", 25);
        PersonWithEquals p3 = new PersonWithEquals("Bob", 30);

        // 1. 自反性
        System.out.println("1. 自反性：x.equals(x) 必须返回 true");
        System.out.println("   p1.equals(p1): " + p1.equals(p1));
        System.out.println("   ✓ 检查通过");

        // 2. 对称性
        System.out.println("\n2. 对称性：x.equals(y) == y.equals(x)");
        boolean xy = p1.equals(p2);
        boolean yx = p2.equals(p1);
        System.out.println("   p1.equals(p2): " + xy);
        System.out.println("   p2.equals(p1): " + yx);
        System.out.println("   ✓ 检查通过: " + (xy == yx));

        // 3. 传递性
        System.out.println(
            "\n3. 传递性：若 x.equals(y)且 y.equals(z)，则 x.equals(z)"
        );
        PersonWithEquals p4 = new PersonWithEquals("Alice", 25);
        boolean c1 = p1.equals(p2);
        boolean c2 = p2.equals(p4);
        boolean c3 = p1.equals(p4);
        System.out.println("   p1.equals(p2): " + c1);
        System.out.println("   p2.equals(p4): " + c2);
        System.out.println("   p1.equals(p4): " + c3);
        System.out.println("   ✓ 检查通过: " + (c1 && c2 && c3));

        // 4. 一致性
        System.out.println("\n4. 一致性：多次调用 x.equals(y) 结果应相同");
        boolean r1 = p1.equals(p2);
        boolean r2 = p1.equals(p2);
        boolean r3 = p1.equals(p2);
        System.out.println("   第一次: " + r1);
        System.out.println("   第二次: " + r2);
        System.out.println("   第三次: " + r3);
        System.out.println("   ✓ 检查通过: " + (r1 == r2 && r2 == r3));

        // 5. 非空检查
        System.out.println("\n5. 非空性：x.equals(null) 必须返回 false");
        System.out.println("   p1.equals(null): " + p1.equals(null));
        System.out.println("   ✓ 检查通过");

        System.out.println();
    }

    /**
     * 演示 hashCode 契约
     */
    private static void demonstrateHashCodeContract() {
        System.out.println("【演示3】hashCode 的两大契约");
        System.out.println("----------------------------------------");

        PersonWithEqualsAndHashCode p1 = new PersonWithEqualsAndHashCode(
            "Alice",
            25
        );
        PersonWithEqualsAndHashCode p2 = new PersonWithEqualsAndHashCode(
            "Alice",
            25
        );
        PersonWithEqualsAndHashCode p3 = new PersonWithEqualsAndHashCode(
            "Bob",
            30
        );

        int hash1 = p1.hashCode();
        int hash2 = p2.hashCode();
        int hash3 = p3.hashCode();

        System.out.println("p1 = Alice,25, hashCode = " + hash1);
        System.out.println("p2 = Alice,25, hashCode = " + hash2);
        System.out.println("p3 = Bob,30, hashCode = " + hash3);

        // 契约1：相等对象必有相同 hashCode
        System.out.println(
            "\n契约1：若 x.equals(y)，则 x.hashCode() == y.hashCode()"
        );
        System.out.println("   p1.equals(p2): " + p1.equals(p2));
        System.out.println(
            "   p1.hashCode() == p2.hashCode(): " + (hash1 == hash2)
        );
        System.out.println("   ✓ 检查通过");

        // 契约2：不相等对象 hashCode 可以相同（碰撞）
        System.out.println(
            "\n契约2：若 !x.equals(y)，x.hashCode() 可以与 y.hashCode() 相同"
        );
        System.out.println("   p1.equals(p3): " + p1.equals(p3));
        System.out.println(
            "   p1.hashCode() == p3.hashCode(): " + (hash1 == hash3)
        );
        System.out.println(
            "   说明：hashCode 相同不代表 equals 相同（哈希碰撞）"
        );

        // 演示哈希碰撞
        System.out.println("\n【哈希碰撞演示】");
        demonstrateHashCollision();

        System.out.println();
    }

    /**
     * 演示哈希碰撞
     */
    private static void demonstrateHashCollision() {
        // 故意构造哈希碰撞（不同的值有相同的 hashCode）
        // "Aa".hashCode() = 2112
        // "BB".hashCode() = 2112
        String s1 = "Aa";
        String s2 = "BB";

        System.out.println("   \"" + s1 + "\".hashCode() = " + s1.hashCode());
        System.out.println("   \"" + s2 + "\".hashCode() = " + s2.hashCode());
        System.out.println("   相同 hashCode，但 equals = " + s1.equals(s2));
        System.out.println("   这就是哈希碰撞，HashMap 会用链表/红黑树解决");

        Set<String> set = new HashSet<>();
        set.add(s1);
        set.add(s2);
        System.out.println(
            "   HashSet 大小 = " + set.size() + "（可以存储两个元素）"
        );
    }

    /**
     * 演示为什么重写 equals 必须重写 hashCode
     */
    private static void demonstrateHashCodeViolation() {
        System.out.println("【演示4】为什么重写 equals 必须重写 hashCode");
        System.out.println("----------------------------------------");

        // 只重写 equals，不重写 hashCode
        PersonWithEqualsOnly p1 = new PersonWithEqualsOnly("Alice", 25);
        PersonWithEqualsOnly p2 = new PersonWithEqualsOnly("Alice", 25);

        System.out.println("p1 = Alice,25");
        System.out.println("p2 = Alice,25");
        System.out.println("p1.equals(p2): " + p1.equals(p2));
        System.out.println(
            "p1.hashCode(): " +
                p1.hashCode() +
                " (默认Object的hashCode，基于地址)"
        );
        System.out.println(
            "p2.hashCode(): " +
                p2.hashCode() +
                " (默认Object的hashCode，基于地址)"
        );
        System.out.println(
            "hashCode 相同: " + (p1.hashCode() == p2.hashCode())
        );

        // 在 HashMap 中测试
        System.out.println("\n测试在 HashMap 中的行为：");
        Map<PersonWithEqualsOnly, String> map = new HashMap<>();
        map.put(p1, "Person 1");
        map.put(p2, "Person 2");

        System.out.println("put(p1, \"Person 1\")");
        System.out.println("put(p2, \"Person 2\")");
        System.out.println(
            "map.size(): " + map.size() + " (预期应该是 1，但实际是 2)！"
        );
        System.out.println("map.get(p1): " + map.get(p1));
        System.out.println("map.get(p2): " + map.get(p2));

        if (map.size() == 2) {
            System.out.println("\n❌ 出问题了！");
            System.out.println(
                "   原因：p1 和 p2 的 hashCode 不同（默认是 Object 基于地址）"
            );
            System.out.println(
                "   它们被放入了 HashMap 的不同桶（index = hash & (n-1)）"
            );
            System.out.println(
                "   虽然 equals 相同，但 HashMap 根据 hashCode 找桶，找错了位置"
            );
        }

        System.out.println("\n对比正确实现：");
        PersonWithBoth p3 = new PersonWithBoth("Alice", 25);
        PersonWithBoth p4 = new PersonWithBoth("Alice", 25);
        Map<PersonWithBoth, String> map2 = new HashMap<>();
        map2.put(p3, "Person 3");
        map2.put(p4, "Person 4");
        System.out.println(
            "正确实现 map.size(): " + map2.size() + " (正确是 1)"
        );
        System.out.println(
            "因为 equals 相同且 hashCode 相同，所以认为是同一个 key"
        );
        System.out.println();
    }

    /**
     * 演示正确的实现方式
     */
    private static void demonstrateCorrectImplementation() {
        System.out.println("【演示5】equals 和 hashCode 的正确实现");
        System.out.println("----------------------------------------");

        // 使用 IDE 生成的代码（推荐）
        ProperPerson p1 = new ProperPerson("Alice", 25, "alice@example.com");
        ProperPerson p2 = new ProperPerson("Alice", 25, "alice@example.com");

        System.out.println("正确实现的类：");
        System.out.println("   public boolean equals(Object o) { ... }");
        System.out.println("   public int hashCode() { ... }");

        System.out.println("\n测试结果：");
        System.out.println("   p1.equals(p2): " + p1.equals(p2));
        System.out.println("   p1.hashCode(): " + p1.hashCode());
        System.out.println("   p2.hashCode(): " + p2.hashCode());
        System.out.println(
            "   hashCode 相同: " + (p1.hashCode() == p2.hashCode())
        );

        // HashMap 测试
        Map<ProperPerson, String> map = new HashMap<>();
        map.put(p1, "Data 1");
        map.put(p2, "Data 2");
        System.out.println("\nHashMap 测试：");
        System.out.println("   map.size(): " + map.size());
        System.out.println("   map.get(p1): " + map.get(p1));
        System.out.println("   ✓ 正确工作！");

        // 使用 Objects 工具类简化
        System.out.println(
            "\n【JDK 7+ 推荐写法】使用 Objects.equals 和 Objects.hash"
        );
        System.out.println("   @Override");
        System.out.println("   public boolean equals(Object o) {");
        System.out.println("       if (this == o) return true;");
        System.out.println(
            "       if (o == null || getClass() != o.getClass()) return false;"
        );
        System.out.println("       ProperPerson that = (ProperPerson) o;");
        System.out.println("       return age == that.age &&");
        System.out.println("              Objects.equals(name, that.name);");
        System.out.println("   }");
        System.out.println();
        System.out.println("   @Override");
        System.out.println("   public int hashCode() {");
        System.out.println("       return Objects.hash(name, age);");
        System.out.println("   }");

        System.out.println();
    }

    /**
     * 演示常见错误
     */
    private static void demonstrateCommonMistakes() {
        System.out.println("【演示6】常见错误和注意事项");
        System.out.println("----------------------------------------");

        System.out.println("❌ 错误1：使用 getClass() 还是 instanceof？");
        System.out.println("   使用 getClass()：必须严格同类型");
        System.out.println("   instanceof：允许子类（可能破坏对称性）");
        System.out.println(
            "   推荐 final 类用 instanceof，非 final 类用 getClass()"
        );

        System.out.println("\n❌ 错误2：equals 参数类型错误");
        System.out.println(
            "   // 错误：public boolean equals(Person o) - 这是重载，不是重写！"
        );
        System.out.println("   // 正确：public boolean equals(Object o)");

        System.out.println("\n❌ 错误3：在 equals 中修改对象状态");
        System.out.println("   equals 应该是只读的，不能修改对象");

        System.out.println("\n❌ 错误4：浮点数比较");
        System.out.println("   Double.compare(double1, double2) 而不是 ==");

        System.out.println("\n❌ 错误5：包含可变对象的 hashCode");
        System.out.println(
            "   如果对象放入 HashSet/HashMap 后修改了参与 hashCode 计算的字段"
        );
        System.out.println("   会导致无法找到该对象！");

        // 可变字段的坑
        System.out.println("\n【可变字段的坑】");
        Set<MutablePerson> set = new HashSet<>();
        MutablePerson mp = new MutablePerson("Alice", 25);
        set.add(mp);
        System.out.println("添加 person (name=Alice, age=25) 到 HashSet");
        System.out.println("HashSet 大小: " + set.size());
        System.out.println("contains(person): " + set.contains(mp));

        // 修改 name（参与 hashCode 的字段）
        mp.name = "Bob";
        System.out.println("\n修改 person.name = \"Bob\"");
        System.out.println("修改后 HashSet 大小: " + set.size());
        System.out.println(
            "contains(person): " + set.contains(mp) + " ❌ 找不到了！"
        );
        System.out.println("原因：修改后 hashCode 变了，去错误的桶找，找不到");

        System.out.println("\n【最佳实践】");
        System.out.println("1. 用于 HashMap/HashSet 的 key 应该是不可变的");
        System.out.println("2. 或者 hashCode 只计算不可变字段");
        System.out.println("3. 使用 @Immutable 或 final 修饰类");
        System.out.println("4. 使用 IDE 自动生成 equals/hashCode");
        System.out.println();
    }

    // ==================== 辅助类 ====================

    /**
     * 基础 Person 类（未重写 equals）
     */
    static class Person {

        String name;
        int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    /**
     * 只重写了 equals 的 Person
     */
    static class PersonWithEquals {

        String name;
        int age;

        PersonWithEquals(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PersonWithEquals that = (PersonWithEquals) o;
            return age == that.age && Objects.equals(name, that.name);
        }
    }

    /**
     * 重写了 equals 和 hashCode 的 Person
     */
    static class PersonWithEqualsAndHashCode {

        String name;
        int age;

        PersonWithEqualsAndHashCode(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PersonWithEqualsAndHashCode that = (PersonWithEqualsAndHashCode) o;
            return age == that.age && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }

    /**
     * 只重写 equals 不重写 hashCode（演示用）
     */
    static class PersonWithEqualsOnly {

        String name;
        int age;

        PersonWithEqualsOnly(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PersonWithEqualsOnly that = (PersonWithEqualsOnly) o;
            return age == that.age && Objects.equals(name, that.name);
        }
        // 故意不重写 hashCode，使用 Object 默认的（基于地址）
    }

    /**
     * 正确实现的 Person（示范）
     */
    static class PersonWithBoth {

        String name;
        int age;

        PersonWithBoth(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PersonWithBoth that = (PersonWithBoth) o;
            return age == that.age && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }

    /**
     * 正确完整的实现（推荐）
     */
    static class ProperPerson {

        private final String name; // 使用 final 确保不可变
        private final int age;
        private final String email;

        ProperPerson(String name, int age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }

        @Override
        public boolean equals(Object o) {
            // 1. 自反性检查
            if (this == o) return true;

            // 2. 非空检查
            if (o == null) return false;

            // 3. 类型检查（使用 getClass 确保严格类型匹配）
            if (getClass() != o.getClass()) return false;

            // 4. 向下转型并比较字段
            ProperPerson that = (ProperPerson) o;
            return (
                age == that.age &&
                Objects.equals(name, that.name) &&
                Objects.equals(email, that.email)
            );
        }

        @Override
        public int hashCode() {
            // Objects.hash 内部处理 null，更安全
            return Objects.hash(name, age, email);
        }

        @Override
        public String toString() {
            return (
                "ProperPerson{" +
                "name='" +
                name +
                '\'' +
                ", age=" +
                age +
                ", email='" +
                email +
                '\'' +
                '}'
            );
        }
    }

    /**
     * 可变 Person（演示 hashCode 陷阱）
     */
    static class MutablePerson {

        String name; // 非 final，可变
        int age;

        MutablePerson(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MutablePerson that = (MutablePerson) o;
            return age == that.age && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age); // 基于可变字段
        }
    }
}

/**
 * equals 与 hashCode 契约总结
 *
 * 【== vs equals】
 * ==              equals
 * 基本类型：值      Object：内存地址
 * 引用类型：地址     重写类：自定义（通常是内容）
 *
 * 【equals 四大契约】
 * 1. 自反性：x.equals(x) == true
 * 2. 对称性：x.equals(y) == y.equals(x)
 * 3. 传递性：x.equals(y) && y.equals(z) => x.equals(z)
 * 4. 一致性：多次调用 x.equals(y) 结果相同
 * 5. 非空：x.equals(null) == false
 *
 * 【hashCode 两大契约】
 * 1. 一致性：多次调用 x.hashCode() 返回相同值（对象未变时）
 * 2. 相等性：x.equals(y) => x.hashCode() == y.hashCode()
 *    （逆命题不成立：hashCode 相同，equals 不一定相同，这叫哈希碰撞）
 *
 * 【为什么重写 equals 必须重写 hashCode】
 * HashMap/HashSet/HashTable 等基于哈希的集合：
 * 1. 先根据 hashCode 找到桶位置（index = hash & (n-1)）
 * 2. 再根据 equals 比较链表中的元素
 * 3. 如果只重写 equals，hashCode 可能不同，导致放入不同桶
 * 4. 即使 equals 相同，也找不到（因为找错桶了）
 *
 * 【实现步骤】
 * 1. 检查是否是同一个对象（this == obj）
 * 2. 检查是否为 null
 * 3. 检查类型是否匹配（getClass() 或 instanceof）
 * 4. 向下转型为当前类
 * 5. 比较关键字段：
 *    - 基本类型用 ==
 *    - 引用类型用 Objects.equals (处理 null)
 *    - float 用 Float.compare
 *    - double 用 Double.compare
 * 6. hashCode 根据 equals 中比较的字段计算
 *
 * 【最佳实践】
 * 1. 使用 IDE 自动生成（Alt + Insert 或右键 Generate）
 * 2. 字段需要参与 equals，就必须参与 hashCode
 * 3. 用于 HashMap key 的对象应该是不可变的
 * 4. 单元测试要验证对称性、传递性、一致性
 * 5. 性能考虑：equals 先比较最可能不同的字段
 * 6. hashCode 计算要分散，避免冲突
 * 7. 使用 Objects.hash(...) 简洁但性能稍差（可变参数）
 *    性能敏感时手动计算：31 * (31 * name.hashCode() + age)
 */
