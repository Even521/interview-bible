/**
 * JIT vs AOT 编译演示与对比
 *
 * JIT (Just-In-Time) - 即时编译：
 * - 在运行时将热点代码编译为机器码
 * - 分层编译：C1（Client Compiler）和 C2（Server Compiler）
 * - 特点：启动快、运行期优化、热点代码检测
 *
 * AOT (Ahead-Of-Time) - 提前编译：
 * - 运行前编译为机器码
 * - 典型代表：GraalVM Native Image
 * - 特点：启动极快、内存占用低、无运行时编译开销
 *
 * @author Java面试宝典
 */
public class JITvsAOTDemo {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("JIT vs AOT 编译技术演示");
        System.out.println("========================================\n");

        // 1. JIT编译原理介绍
        demonstrateJIT();

        // 2. AOT编译原理介绍
        demonstrateAOT();

        // 3. JIT热点代码检测演示
        demonstrateHotSpotJIT();

        // 4. 性能对比场景
        demonstratePerformanceComparison();

        // 5. 使用场景分析
        demonstrateUseCases();

        // 6. JVM参数配置
        demonstrateJVMParameters();

        // 7. GraalVM使用示例
        demonstrateGraalVM();

        System.out.println("\n========================================");
        System.out.println("演示结束！");
        System.out.println("========================================");
    }

    /**
     * JIT即时编译原理
     */
    private static void demonstrateJIT() {
        System.out.println("【JIT 即时编译原理】");
        System.out.println("----------------------------------------");

        System.out.println("JIT (Just-In-Time) 工作流程：");
        System.out.println();

        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│ 1. 解释执行阶段                          │");
        System.out.println("│    - 字节码由 Interpreter 逐行解释执行  │");
        System.out.println("│    - 启动快，但执行慢                     │");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.println("                    ↓ 执行次数达到阈值");
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│ 2. C1 编译（Client Compiler）            │");
        System.out.println("│    - 方法调用次数 > 1500 次              │");
        System.out.println("│    - 简单优化（方法内联、去虚拟化等）      │");
        System.out.println("│    - 编译速度快，优化程度中等              │");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.println("                    ↓ 执行次数进一步增加");
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│ 3. C2 编译（Server Compiler）            │");
        System.out.println("│    - 方法调用次数 > 10000 次             │");
        System.out.println("│    - 激进优化（逃逸分析、循环展开等）      │");
        System.out.println("│    - 编译速度慢，但生成代码质量高          │");
        System.out.println("└─────────────────────────────────────────┘");

        System.out.println("\n【分层编译等级】（Java 8+ 默认开启 Tiered Compilation）");
        System.out.println(" Level 0: 解释执行");
        System.out.println(" Level 1: C1 + 全 profiling（ profiling 全开）");
        System.out.println(" Level 2: C1 + 调用/分支 profiling");
        System.out.println(" Level 3: C1 + 全 profiling（默认大部分方法）");
        System.out.println(" Level 4: C2 编译（热点方法）");
        System.out.println();

        System.out.println("【JIT编译触发条件】");
        System.out.println(" - 方法调用计数器：方法被调用的次数");
        System.out.println(" - 回边计数器：循环体代码执行次数");
        System.out.println(" - 计算公式：调用次数 × OSR因子（循环体）");
        System.out.println();

        System.out.println("【JIT优化技术】");
        System.out.println(" 1. 方法内联（Method Inlining）");
        System.out.println("    - 将短小的方法体直接插入调用处");
        System.out.println("    - 消除方法调用开销");
        System.out.println("    - 默认最大内联字节码 325 bytes（-XX:MaxInlineSize）");
        System.out.println();

        System.out.println(" 2. 逃逸分析（Escape Analysis）");
        System.out.println("    - 判断对象是否逃逸出方法/线程");
        System.out.println("    - 栈上分配：对象在栈上分配而非堆");
        System.out.println("    - 标量替换：将对象拆分为基本类型");
        System.out.println("    - 锁消除：无逃逸则去除同步锁");
        System.out.println();

        System.out.println(" 3. 去虚拟化（Devirtualization）");
        System.out.println("    - 类层次分析（CHA）确定唯一实现类");
        System.out.println("    - 将虚方法调用优化为直接调用");
        System.out.println();

        System.out.println(" 4. 循环优化");
        System.out.println("    - 循环展开（Loop Unrolling）");
        System.out.println("    - 范围检查消除");
        System.out.println("    - 向量化（SIMD）");
        System.out.println();
    }

    /**
     * AOT提前编译原理
     */
    private static void demonstrateAOT() {
        System.out.println("【AOT 提前编译原理】");
        System.out.println("----------------------------------------");

        System.out.println("AOT (Ahead-Of-Time) 工作流程：");
        System.out.println();

        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│ 源代码 (.java)                          │");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.println("                    ↓ 编译");
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│ 字节码 (.class)                         │");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.println("                    ↓ AOT编译");
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│ 机器码（平台相关二进制）                  │");
        System.out.println("│ - 静态链接所有依赖                       │");
        System.out.println("│ - 包含 Substrate VM 运行时              │");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.println("                    ↓ ");
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│ 直接执行，无JVM启动过程                   │");
        System.out.println("│ - 启动时间 < 10ms                       │");
        System.out.println("│ - 内存占用大幅减少                        │");
        System.out.println("└─────────────────────────────────────────┘");

        System.out.println("\n【AOT实现方案】");
        System.out.println(" 1. GraalVM Native Image（主流）");
        System.out.println("    - 将Java应用编译为本地可执行文件");
        System.out.println("    - 静态分析确定可达代码");
        System.out.println("    - 包含 Substrate VM 轻量级运行时");
        System.out.println();

        System.out.println(" 2. Excelsior JET");
        System.out.println("    - 传统AOT编译器");
        System.out.println("    - 需要JRT运行时");
        System.out.println();

        System.out.println(" 3. Android ART (Android Runtime)");
        System.out.println("    - Android 5.0+ 默认运行时");
        System.out.println("    - 安装时AOT编译（.dex → .oat）");
        System.out.println();

        System.out.println("【AOT的限制】");
        System.out.println(" 1. 动态特性受限");
        System.out.println("    - 反射需要配置");
        System.out.println("    - 动态代理需要配置");
        System.out.println("    - JNI需要配置");
        System.out.println("    - 资源文件需要显式包含");
        System.out.println();

        System.out.println(" 2. ClassLoader限制");
        System.out.println("    - 不支持自定义ClassLoader动态加载");
        System.out.println("    - 不支持运行时字节码生成（字节码插桩）");
        System.out.println();

        System.out.println(" 3. GC选择受限");
        System.out.println("    - 默认使用 Serial GC");
        System.out.println("    - 可选G1 GC，但性能不如JVM");
        System.out.println();

        System.out.println(" 4. 编译时间长");
        System.out.println("    - 静态分析耗时长");
        System.out.println("    - 编译后可执行文件较大（包含运行时）");
        System.out.println();

        System.out.println(" 5. 平台相关");
        System.out.println("    - 编译后的二进制与平台绑定");
        System.out.println("    - 跨平台需要分别编译");
        System.out.println();
    }

    /**
     * JIT热点代码检测演示
     */
    private static void demonstrateHotSpotJIT() {
        System.out.println("【JIT 热点代码检测演示】");
        System.out.println("----------------------------------------");
        System.out.println("执行以下代码可观察JIT编译过程：");
        System.out.println();

        System.out.println("// JVM参数：-XX:+PrintCompilation -XX:+CITime");
        System.out.println("public class HotSpotDemo {");
        System.out.println("    public int calculate(int x) {");
        System.out.println("        return x * x + 2 * x + 1;");
        System.out.println("    }");
        System.out.println("    ");
        System.out.println("    public static void main(String[] args) {");
        System.out.println("        HotSpotDemo demo = new HotSpotDemo();");
        System.out.println("        for (int i = 0; i < 100000; i++) {");
        System.out.println("            demo.calculate(i); // 热点代码");
        System.out.println("        }");
        System.out.println("    }");
        System.out.println("}");
        System.out.println();

        // 模拟热点方法
        System.out.println("【模拟执行热点方法】");
        System.out.print("执行 calculate 方法 20000 次...");

        HotSpotMethod method = new HotSpotMethod();
        long start = System.currentTimeMillis();

        // 预热
        for (int i = 0; i < 1000; i++) {
            method.calculate(i);
        }

        // 正式执行
        for (int i = 0; i < 20000; i++) {
            method.calculate(i);
        }

        long end = System.currentTimeMillis();
        System.out.println(" 耗时: " + (end - start) + " ms");
        System.out.println();

        System.out.println("【编译日志解读】");
        System.out.println("-XX:+PrintCompilation 输出示例：");
        System.out.println("   1635   45   %     3       java.lang.StringBuilder::append (8 bytes)");
        System.out.println("   1844   63   %     4       java.util.ArrayList::grow @ 50 (45 bytes)");
        System.out.println("   |      |    |     |        |                  |      |    |");
        System.out.println("   |      |    |     |        |                  |      |    `- 方法大小(bytes)");
        System.out.println("   |      |    |     |        |                  |      `- 被OSR编译的位置");
        System.out.println("   |      |    |     |        |                  `- 方法名");
        System.out.println("   |      |    |     |        `- 类名");
        System.out.println("   |      |    |     `- 编译级别(0-4, %表示OSR)");
        System.out.println("   |      |    `- %表示on-stack replacement(OSR)编译");
        System.out.println("   |      `- JIT编译ID");
        System.out.println("   `- JVM启动后的毫秒数");
        System.out.println();

        System.out.println("编译级别说明：");
        System.out.println(" 0 - Interpreter 解释执行");
        System.out.println(" 1 - Simple C1 编译");
        System.out.println(" 2 - Limited C1 编译（带profiling）");
        System.out.println(" 3 - Full C1 编译（带profiling）");
        System.out.println(" 4 - C2 编译（激进优化）");
        System.out.println();
    }

    /**
     * 热点方法类
     */
    static class HotSpotMethod {
        // 简单计算，可被JIT优化
        public int calculate(int x) {
            return x * x + 2 * x + 1;
        }
    }

    /**
     * JIT vs AOT 性能对比
     */
    private static void demonstratePerformanceComparison() {
        System.out.println("【JIT vs AOT 性能对比】");
        System.out.println("----------------------------------------");

        System.out.println("┌─────────────────┬──────────────┬──────────────┐");
        System.out.println("│ 指标            │ JIT (HotSpot)│ AOT (Native) │");
        System.out.println("├─────────────────┼──────────────┼──────────────┤");
        System.out.println("│ 启动时间         │ 1-5 秒        │ < 10 ms      │");
        System.out.println("│ 内存占用         │ 高（JVM开销）  │ 极低（无JVM） │");
        System.out.println("│ 峰值性能         │ ⭐⭐⭐⭐⭐        │ ⭐⭐⭐         │");
        System.out.println("│ 稳定性能         │ ⭐⭐⭐⭐         │ ⭐⭐⭐⭐        │");
        System.out.println("│ 代码优化         │ 运行时优化      │ 编译期优化    │");
        System.out.println("│ 动态特性         │ ✅ 支持        │ ❌ 受限       │");
        System.out.println("│ 反射             │ ✅ 原生支持    │ ⚠️ 需配置    │");
        System.out.println("│ 跨平台           │ ✅ 字节码跨平台 │ ❌ 平台相关   │");
        System.out.println("│ 编译时间         │ 运行期自动      │ 编译期较长    │");
        System.out.println("│ 包体积           │ 小            │ 大（含运行时） │");
        System.out.println("│ 调试诊断         │ ⭐⭐⭐⭐⭐        │ ⭐⭐          │");
        System.out.println("└─────────────────┴──────────────┴──────────────┘");
        System.out.println();

        System.out.println("【启动时间对比】");
        System.out.println("  Spring Boot App + JVM:     5-15 秒");
        System.out.println("  Spring Boot Native Image:   0.1 秒");
        System.out.println("  提升: 50-150 倍");
        System.out.println();

        System.out.println("【内存占用对比】");
        System.out.println("  Spring Boot JVM:            100-500 MB");
        System.out.println("  Spring Boot Native:         20-50 MB");
        System.out.println("  节省: 60-80%");
        System.out.println();

        System.out.println("【吞吐量对比】");
        System.out.println("  JVM JIT:                    峰值性能高（优化充分后）");
        System.out.println("  Native AOT:                 约是JVM的80-90%");
        System.out.println("  差距: 10-20%（JIT运行时优化的优势）");
        System.out.println();

        System.out.println("【适用场景对比】");
        System.out.println("  JIT优势场景：");
        System.out.println("    - 长时间运行的服务（性能敏感）");
        System.out.println("    - 需要动态优化的应用（复杂业务逻辑）");
        System.out.println("    - 开发阶段（热更新、调试便利）");
        System.out.println("    - 使用大量反射/动态代理");
        System.out.println();

        System.out.println("  AOT优势场景：");
        System.out.println("    - Serverless/函数计算（快速启动）");
        System.out.println("    - 微服务架构（快速扩缩容）");
        System.out.println("    - 命令行工具（启动快）");
        System.out.println("    - 云原生应用（容器镜像小）");
        System.out.println("    - 嵌入式/IoT（内存受限）");
        System.out.println();
    }

    /**
     * 使用场景分析
     */
    private static void demonstrateUseCases() {
        System.out.println("【JIT vs AOT 使用场景分析】");
        System.out.println("----------------------------------------");

        System.out.println("场景1：微服务架构");
        System.out.println("┌─────────────────────────────────────┐");
        System.out.println("│ 推荐：Spring Boot + Native Image   │");
        System.out.println("│ - 快速启动（<1秒）");
        System.out.println("│ - 快速扩缩容，适合K8s");
        System.out.println("│ - 容器镜像小（Alpine基础镜像）");
        System.out.println("└─────────────────────────────────────┘");
        System.out.println();

        System.out.println("场景2：传统大型单体应用");
        System.out.println("┌─────────────────────────────────────┐");
        System.out.println("│ 推荐：传统JVM + JIT编译             │");
        System.out.println("│ - 长期运行，JIT充分优化性能");
        System.out.println("│ - 复杂反射、动态代理场景");
        System.out.println("│ - 需要JVM调优和诊断");
        System.out.println("└─────────────────────────────────────┘");
        System.out.println();

        System.out.println("场景3：Serverless函数");
        System.out.println("┌─────────────────────────────────────┐");
        System.out.println("│ 推荐：Native Image（必选）         │");
        System.out.println("│ - 冷启动时间决定成本");
        System.out.println("│ - 无预付启动时间惩罚");
        System.out.println("│ - AWS Lambda / Azure Functions适配  │");
        System.out.println("└─────────────────────────────────────┘");
        System.out.println();

        System.out.println("场景4：命令行工具/CLI");
        System.out.println("┌─────────────────────────────────────┐");
        System.out.println("│ 推荐：Native Image                  │");
        System.out.println("│ - 毫秒级启动，用户无感知");
        System.out.println("│ - 单二进制分发，无依赖");
        System.out.println("│ - 示例：Maven、Gradle、Quarkus CLI  │");
        System.out.println("└─────────────────────────────────────┘");
        System.out.println();

        System.out.println("场景5：高性能计算");
        System.out.println("┌─────────────────────────────────────┐");
        System.out.println("│ 推荐：JVM + JIT + Graal Compiler    │");
        System.out.println("│ - 峰值性能关键");
        System.out.println("│ - 长时间预热获得最佳性能");
        System.out.println("│ - GraalVM CE/EE 提供顶级性能");
        System.out.println("└─────────────────────────────────────┘");
        System.out.println();

        System.out.println("场景6：Android App");
        System.out.println("┌─────────────────────────────────────┐");
        System.out.println("│ 推荐：ART AOT编译                    │");
        System.out.println("│ - 安装时dex2oat编译");
        System.out.println("│ - 运行时AOT + JIT混合");
        System.out.println("└─────────────────────────────────────┘");
        System.out.println();
    }

    /**
     * JVM参数配置
     */
    private static void demonstrateJVMParameters() {
        System.out.println("【JIT/AOT JVM参数配置】");
        System.out.println("----------------------------------------");

        System.out.println("【JIT相关参数】");
        System.out.println();

        System.out.println("1. 关闭JIT（不推荐，仅调试用）：");
        System.out.println("   -Xint          # 纯解释执行");
        System.out.println("   -Xcomp         # 纯编译执行（启动慢）");
        System.out.println();

        System.out.println("2. JIT编译阈值设置：");
        System.out.println("   -XX:CompileThreshold=10000    # 触发C2编译的调用次数（默认10000）");
        System.out.println("   -XX:TieredCompileAtLevel=...  # 指定分层编译级别");
        System.out.println();

        System.out.println("3. JIT编译器选择：");
        System.out.println("   -client                       # 使用C1（已废弃）");
        System.out.println("   -server                       # 使用C2（默认）");
        System.out.println("   -XX:+TieredCompilation         # 分层编译（默认开启）");
        System.out.println();

        System.out.println("4. 编译日志：");
        System.out.println("   -XX:+PrintCompilation          # 打印方法编译信息");
        System.out.println("   -XX:+CITime                    # 打印编译耗时统计");
        System.out.println("   -XX:+LogCompilation            # 详细编译日志");
        System.out.println("   -XX:LogFile=jit.log            # 输出到文件");
        System.out.println();

        System.out.println("5. 编译限制：");
        System.out.println("   -XX:ReservedCodeCacheSize=256m # 代码缓存大小");
        System.out.println("   -XX:MaxInlineSize=325         # 最大内联方法大小（字节码bytes）");
        System.out.println("   -XX:FreqInlineSize=325         # 频繁调用方法的最大内联大小");
        System.out.println();

        System.out.println("6. 逃逸分析：");
        System.out.println("   -XX:+DoEscapeAnalysis            # 开启逃逸分析（默认开启）");
        System.out.println("   -XX:+EliminateAllocations        # 开启标量替换（默认开启）");
        System.out.println("   -XX:+EliminateLocks              # 开启锁消除（默认开启）");
        System.out.println();

        System.out.println("【AOT相关参数】");
        System.out.println();

        System.out.println("1. GraalVM Native Image基础命令：");
        System.out.println("   # 安装GraalVM和native-image工具");
        System.out.println("   gu install native-image");
        System.out.println();
        System.out.println("   # 编译命令");
        System.out.println("   native-image -cp your-app.jar com.example.Main \\");
        System.out.println("                -H:Name=your-app \\");
        System.out.println("                -H:Class=com.example.Main \\");
        System.out.println("                --no-fallback \\");
        System.out.println("                -H:+ReportExceptionStackTraces");
        System.out.println();

        System.out.println("2. Spring Boot Native Image（推荐）：");
        System.out.println("   # Maven插件");
        System.out.println("   mvn spring-boot:build-image -Pnative");
        System.out.println();
        System.out.println("   # 或使用Buildpacks");
        System.out.println("   mvn spring-boot:build-image -Dspring-boot.build-image.imageName=myapp:latest");
        System.out.println();

        System.out.println("3. 反射配置（reflect-config.json）：");
        System.out.println("   [");
        System.out.println("     {");
        System.out.println("       \"name\": \"com.example.User\",");
        System.out.println("       \"allDeclaredFields\": true,");
        System.out.println("       \"allDeclaredMethods\": true");
        System.out.println("     }");
        System.out.println("   ]");
        System.out.println();

        System.out.println("4. 资源文件配置（resource-config.json）：");
        System.out.println("   {");
        System.out.println("     \"resources\": {");
        System.out.println("       \"includes\": [{\"pattern\": \"application.yml\"}]");
        System.out.println("     }");
        System.out.println("   }");
        System.out.println();

        System.out.println("5. Native Image优化参数：");
        System.out.println("   --enable-monitoring=heapdump,gc  # 开启JMX支持");
        System.out.println("   --initialize-at-build-time=com.example  # 构建时初始化");
        System.out.println("   --initialize-at-run-time=com.example.Dyn # 运行时初始化");
        System.out.println("   -H:+AddAllCharsets                # 包含所有字符集");
        System.out.println("   -H:+JNI                          # 启用JNI支持");
        System.out.println();
    }

    /**
     * GraalVM使用示例
     */
    private static void demonstrateGraalVM() {
        System.out.println("【GraalVM AOT Native Image 实战示例】");
        System.out.println("----------------------------------------");

        System.out.println("【步骤1：安装GraalVM】");
        System.out.println("  # 下载GraalVM CE");
        System.out.println("  wget https://github.com/graalvm/graalvm-ce-builds/releases/download/...");
        System.out.println("  ");
        System.out.println("  # 配置环境变量");
        System.out.println("  export JAVA_HOME=/path/to/graalvm-ce-java17-22.1.0");
        System.out.println("  export PATH=$JAVA_HOME/bin:$PATH");
        System.out.println("  ");
        System.out.println("  # 安装native-image工具");
        System.out.println("  gu install native-image");
        System.out.println();

        System.out.println("【步骤2：准备Java应用】");
        System.out.println("  // HelloWorld.java");
        System.out.println("  public class HelloWorld {");
        System.out.println("      public static void main(String[] args) {");
        System.out.println("          System.out.println(\"Hello, Native Image!\");");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println("  ");
        System.out.println("  $ javac HelloWorld.java");
        System.out.println("  $ java HelloWorld");
        System.out.println("  -> Hello, Native Image!");
        System.out.println();

        System.out.println("【步骤3：AOT编译为Native可执行文件】");
        System.out.println("  $ native-image HelloWorld");
        System.out.println("  ");
        System.out.println("  输出：");
        System.out.println("  ========================================================================");
        System.out.println("  GraalVM Native Image: Generating 'helloworld'...");
        System.out.println("  ========================================================================");
        System.out.println("  [1/7] Initializing...");
        System.out.println("  [2/7] Performing analysis...");
        System.out.println("  [3/7] Building universe...");
        System.out.println("  [4/7] Parsing methods...");
        System.out.println("  [5/7] Inlining methods...");
        System.out.println("  [6/7] Compiling methods...");
        System.out.println("  [7/7] Creating image...");
        System.out.println("  ------------------------------------------------------------------------");
        System.out.println("  Build artifacts: helloworld");
        System.out.println("  ========================================================================");
        System.out.println("  ");
        System.out.println("  $ ls -lh helloworld");
        System.out.println("  -rwxr-xr-x  1 user  group   4.2M  Aug  1 10:00 helloworld");
        System.out.println();

        System.out.println("【步骤4：运行Native程序】");
        System.out.println("  # 对比：JVM方式");
        System.out.println("  $ time java HelloWorld");
        System.out.println("  Hello, Native Image!");
        System.out.println("  java HelloWorld  0.05s user 0.02s system 10% cpu 0.500 total");
        System.out.println("  ");
        System.out.println("  # AOT Native方式");
        System.out.println("  $ time ./helloworld");
        System.out.println("  Hello, Native Image!");
        System.out.println("  ./helloworld  0.002s user 0.001s system 80% cpu 0.003 total");
        System.out.println("  ");
        System.out.println("  提升：启动时间从 0.5s 降低到 0.003s，提升约 167 倍！");
        System.out.println();

        System.out.println("【步骤5：Spring Boot + Native Image】");
        System.out.println("  # pom.xml 配置");
        System.out.println("  <build>");
        System.out.println("    <plugins>");
        System.out.println("      <plugin>");
        System.out.println("        <groupId>org.springframework.boot</groupId>");
        System.out.println("        <artifactId>spring-boot-maven-plugin</artifactId>");
        System.out.println("        <configuration>");
        System.out.println("          <image>");
        System.out.println("            <builder>paketobuildpacks/builder:tiny</builder>");
        System.out.println("          </image>");
        System.out.println("        </configuration>");
        System.out.println("      </plugin>");
        System.out.println("      <plugin>");
        System.out.println("        <groupId>org.graalvm.buildtools</groupId>");
        System.out.println("        <artifactId>native-maven-plugin</artifactId>");
        System.out.println("      </plugin>");
        System.out.println("    </plugins>");
        System.out.println("  </build>");
        System.out.println("  ");
        System.out.println("  # 构建");
        System.out.println("  $ mvn native:compile -Pnative");
        System.out.println("  ");
        System.out.println("  # 产物");
        System.out.println("  $ target/myapp");
        System.out.println("  > Started Application in 0.085 seconds");
        System.out.println();

        System.out.println("【Spring Boot JVM vs Native 对比】");
        System.out.println("┌──────────────────┬───────────────┬────────────────┐");
        System.out.println("│ 指标             │ JVM (传统)    │ Native Image   │");
        System.out.println("├──────────────────┼───────────────┼────────────────┤");
        System.out.println("│ Jar包大小         │ 50 MB        │ N/A            │");
        System.out.println("│ Native可执行文件   │ N/A          │ 80 MB (含JVM)   │");
        System.out.println("│ 启动时间          │ 3.5 秒        │ 0.1 秒          │");
        System.out.println("│ 内存占用（空闲）   │ 200 MB       │ 40 MB          │");
        System.out.println("│ 首次请求延迟       │ 3.5 秒(冷启动)│ 0.1 秒          │");
        System.out.println("│ 吞吐量            │ 5000 req/s   │ 4500 req/s      │");
        System.out.println("│ JIT预热时间        │ 5 分钟        │ 无(编译期完成)   │");
        System.out.println("└──────────────────┴───────────────┴────────────────┘");
        System.out.println();

        System.out.println("【Native Image 最佳实践】");
        System.out.println("  1. 使用 Spring Boot 3.x + GraalVM");
        System.out.println("  2. 配置 reachability-metadata（反射、资源）");
        System.out.println("  3. 构建时使用Buildpacks自动化");
        System.out.println("  4. 配合Docker多阶段构建：");
        System.out.println("     FROM ghcr.io/graalvm/graalvm-ce:ol8-java17 AS builder");
        System.out.println("     FROM gcr.io/distroless/static:nonroot");
        System.out.println("     COPY --from=builder /app/target/myapp /");
        System.out.println("     ENTRYPOINT [\"/myapp\"]");
        System.out.println();
    }
}

/**
 * JIT vs AOT 核心知识点总结
 *
 * 【JIT 核心】
 * - 在运行时将热点代码编译为机器码
 * - 分层编译：C1（快速）+ C2（深度优化）
 * - 关键优化：方法内联、逃逸分析、去虚拟化、循环展开
 * - 优点：峰值性能高、动态优化、跨平台
 * - 缺点：启动慢、预热时间长、内存占用大
 *
 * 【AOT 核心】
 * - 编译期将字节码编译为机器码
 * - 代表技术：GraalVM Native Image
 * - 特点：启动极快、内存占用低、无运行时编译
 * - 限制：反射需配置、动态特性受限、平台绑定
 *
 * 【选择策略】
 * 启动敏感：AOT（微服务、Serverless、CLI）
 * 峰值性能：JIT（高性能计算、大数据）
 * 混合方案：GraalVM提供JIT（HotSpot）+ AOT（Native Image）双模式
 *
 * 【面试常问】
 * 1. JIT分层编译有几个等级？（0-4级）
 * 2. JIT逃逸分析的作用？（栈上分配、锁消除）
 * 3. AOT的主要限制是什么？（反射需配置、动态特性受限）
 * 4. 什么时候选Native Image？（Serverless、微服务快速扩缩容）
 * 5. TC编译的触发条件？（方法调用次数、回边计数）
 */
```

\java\code\OnlineTroubleshootingDemo.java
