package code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * ArrayList 和 LinkedList 对比演示
 *
 * 核心知识点：
 * 1. 底层数据结构：ArrayList(动态数组) vs LinkedList(双向链表)
 * 2. 扩容机制：ArrayList 1.5倍扩容 vs LinkedList 无需扩容
 * 3. 性能差异：随机访问 vs 顺序访问，插入删除性能
 * 4. 内存占用：ArrayList(连续内存，指针少) vs LinkedList(节点分散，指针多)
 * 5. 适用场景：查询多 vs 增删多
 *
 * @author Java面试宝典
 */
public class ArrayListVsLinkedListDemo {

	public static void main(String[] args) {
		System.out.println("========================================");
		System.out.println("ArrayList vs LinkedList 对比演示");
		System.out.println("========================================\n");

		// 1. 底层数据结构
		demonstrateDataStructure();

		// 2. 扩容机制
		demonstrateGrowthMechanism();

		// 3. 访问性能对比
		demonstrateAccessPerformance();

		// 4. 插入删除性能对比
		demonstrateModifyPerformance();

		// 5. 内存占用对比
		demonstrateMemoryUsage();

		// 6. 特性和使用场景
		demonstrateFeatures();

		// 7. 迭代器性能
		demonstrateIteratorPerformance();

		System.out.println("\n========================================");
		System.out.println("演示结束！");
		System.out.println("========================================");
	}

	/**
	 * 演示底层数据结构
	 */
	private static void demonstrateDataStructure() {
		System.out.println("【演示1】底层数据结构对比");
		System.out.println("----------------------------------------");

		System.out.println("ArrayList 底层结构：");
		System.out.println(" Object[] elementData; // 动态数组");
		System.out.println(" int size; // 实际元素数量");
		System.out.println(" int DEFAULT_CAPACITY = 10; // 默认容量");
		System.out.println(" 内存布局：[0][1][2][3][4][...][n]");
		System.out.println(" 特点：元素连续存储，支持随机访问");

		System.out.println("\nLinkedList 底层结构：");
		System.out.println(" Node<E> first; // 头节点");
		System.out.println(" Node<E> last; // 尾节点");
		System.out.println(" int size; // 实际元素数量");
		System.out.println(" 节点结构：");
		System.out.println("   class Node<E> {");
		System.out.println("     E item; // 数据");
		System.out.println("     Node<E> next; // 后继指针");
		System.out.println("     Node<E> prev; // 前驱指针");
		System.out.println("   }");
		System.out.println(" 内存布局：[数据|next|prev] <-> [数据|next|prev] <-> ...");
		System.out.println(" 特点：节点分散存储，只能顺序访问");

		// 可视化
		List<String> arrayList = new ArrayList<>();
		arrayList.add("A");
		arrayList.add("B");
		arrayList.add("C");
		System.out.println("\nArrayList: " + arrayList);
		System.out.println("底层数组位置: index0=A, index1=B, index2=C");

		List<String> linkedList = new LinkedList<>();
		linkedList.add("A");
		linkedList.add("B");
		linkedList.add("C");
		System.out.println("\nLinkedList: " + linkedList);
		System.out.println("底层节点: A <-> B <-> C");
		System.out.println();
	}

	/**
	 * 演示扩容机制
	 */
	private static void demonstrateGrowthMechanism() {
		System.out.println("【演示2】扩容机制对比");
		System.out.println("----------------------------------------");

		System.out.println("ArrayList 扩容机制：");
		System.out.println(" 1. 初始容量：10（默认）");
		System.out.println(" 2. 扩容时机：size == elementData.length");
		System.out.println(" 3. 扩容策略：grow(oldCapacity + (oldCapacity >> 1))");
		System.out.println("    即：新容量 = 旧容量 + 旧容量/2 = 旧容量的 1.5倍");
		System.out.println(" 4. 扩容操作：Arrays.copyOf(elementData, newCapacity)");
		System.out.println("    创建新数组，复制旧数据，O(n) 时间复杂度");
		System.out.println(" 5. 手动扩容：ensureCapacity(int minCapacity)");

		// 扩容源码简化
		System.out.println("\n【ArrayList grow() 源码简化】");
		System.out.println("private void grow(int minCapacity) {");
		System.out.println("  int oldCapacity = elementData.length;");
		System.out.println("  int newCapacity = oldCapacity + (oldCapacity >> 1); // 1.5倍");
		System.out.println("  if (newCapacity - minCapacity < 0)");
		System.out.println("    newCapacity = minCapacity;");
		System.out.println("  elementData = Arrays.copyOf(elementData, newCapacity);");
		System.out.println("}");

		System.out.println("\nLinkedList 扩容机制：");
		System.out.println(" 无需扩容！");
		System.out.println(" 每次添加元素直接创建新 Node 节点");
		System.out.println(" 没有容量概念，理论上无限扩展（受限于内存）");
		System.out.println(" 添加操作：O(1)，只需要修改指针");

		// 演示扩容过程
		System.out.println("\n【扩容演示】");
		ArrayList<Integer> list = new ArrayList<>(2); // 初始容量2
		System.out.println("创建 ArrayList，初始容量=2");

		for (int i = 0; i < 5; i++) {
			int oldCapacity = getArrayListCapacity(list);
			list.add(i);
			int newCapacity = getArrayListCapacity(list);
			if (oldCapacity != newCapacity) {
				System.out.println(" 添加元素 " + i + " 触发扩容: " +
					oldCapacity + " -> " + newCapacity);
			} else {
				System.out.println(" 添加元素 " + i + ", 容量=" + newCapacity);
			}
		}
		System.out.println();
	}

	/**
	 * 获取 ArrayList 容量（反射）
	 */
	private static int getArrayListCapacity(ArrayList<?> list) {
		try {
			java.lang.reflect.Field field = ArrayList.class.getDeclaredField("elementData");
			field.setAccessible(true);
			Object[] elementData = (Object[]) field.get(list);
			return elementData.length;
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * 演示访问性能
	 */
	private static void demonstrateAccessPerformance() {
		System.out.println("【演示3】访问性能对比（随机访问 vs 顺序访问）");
		System.out.println("----------------------------------------");

		int size = 100000;
		System.out.println("数据量: " + size);

		List<Integer> arrayList = new ArrayList<>(size);
		List<Integer> linkedList = new LinkedList<>();

		// 初始化数据
		for (int i = 0; i < size; i++) {
			arrayList.add(i);
			linkedList.add(i);
		}

		// 测试随机访问 ArrayList
		long start = System.currentTimeMillis();
		for (int i = 0; i < size; i++) {
			arrayList.get(i); // O(1)
		}
		long arrayListRandomAccess = System.currentTimeMillis() - start;
		System.out.println("\nArrayList 随机访问 " + size + " 次: " +
			arrayListRandomAccess + " ms (O(1))");

		// 测试随机访问 LinkedList（很慢）
		start = System.currentTimeMillis();
		for (int i = 0; i < size; i++) {
			linkedList.get(i); // O(n)，因为每次都要从头遍历
		}
		long linkedListRandomAccess = System.currentTimeMillis() - start;
		System.out.println("LinkedList 随机访问 " + size + " 次: " +
			linkedListRandomAccess + " ms (O(n)) - 极慢！");

		// 测试顺序访问 LinkedList（用迭代器，很快）
		start = System.currentTimeMillis();
		for (Integer num : linkedList) { // 使用迭代器，O(1) per element
			// do nothing
		}
		long linkedListSequentialAccess = System.currentTimeMillis() - start;
		System.out.println("LinkedList 顺序访问（迭代器）" + size + " 次: " +
			linkedListSequentialAccess + " ms (O(n) but fast)");

		System.out.println("\n【结论】");
		System.out.println("随机访问：ArrayList O(1) 远快于 LinkedList O(n)");
		System.out.println("顺序访问：两者都是 O(n)，ArrayList 略快（数组连续，CPU缓存友好）");
		System.out.println("LinkedList.get(index) 需要从头或尾遍历到指定位置，O(n)");
		System.out.println();
	}

	/**
	 * 演示插入删除性能
	 */
	private static void demonstrateModifyPerformance() {
		System.out.println("【演示4】插入删除性能对比");
		System.out.println("----------------------------------------");

		System.out.println("时间复杂度分析：");
		System.out.println("┌─────────────────┬──────────────┬───────────────┐");
		System.out.println("│     操作        │  ArrayList   │  LinkedList   │");
		System.out.println("├─────────────────┼──────────────┼───────────────┤");
		System.out.println("│ add(E) 尾部     │    O(1)*     │     O(1)      │");
		System.out.println("│ add(index, E)   │    O(n)      │     O(n)      │");
		System.out.println("│ 头部/中部插入   │    O(n)      │     O(1)**    │");
		System.out.println("│ remove(index)   │    O(n)      │     O(n)      │");
		System.out.println("│ 头部/中部删除   │    O(n)      │     O(1)**    │");
		System.out.println("└─────────────────┴──────────────┴───────────────┘");
		System.out.println("* 可能需要扩容触发 O(n)");
		System.out.println("** 需要找到位置 O(n)，修改指针 O(1)");

		int size = 50000;
		System.out.println("\n数据量: " + size);

		// 测试头部插入
		System.out.println("\n1. 头部插入性能：");
		List<Integer> arrayList = new ArrayList<>();
		List<Integer> linkedList = new LinkedList<>();

		long start = System.currentTimeMillis();
		for (int i = 0; i < 10000; i++) {
			arrayList.add(0, i); // 头部插入，需要移动所有元素
		}
		long arrayListHeadInsert = System.currentTimeMillis() - start;
		System.out.println("ArrayList 头部插入 10000 次: " +
			arrayListHeadInsert + " ms (移动元素开销大)");

		start = System.currentTimeMillis();
		for (int i = 0; i < 10000; i++) {
			linkedList.add(0, i); // 头部插入，只需要修改指针
		}
		long linkedListHeadInsert = System.currentTimeMillis() - start;
		System.out.println("LinkedList 头部插入 10000 次: " +
			linkedListHeadInsert + " ms (修改指针很快)");

		// 测试尾部插入
		System.out.println("\n2. 尾部插入性能：");
		arrayList.clear();
		linkedList.clear();

		start = System.currentTimeMillis();
		for (int i = 0; i < size; i++) {
			arrayList.add(i); // 尾部插入
		}
		long arrayListTailInsert = System.currentTimeMillis() - start;
		System.out.println("ArrayList 尾部插入: " +
			arrayListTailInsert + " ms");

		start = System.currentTimeMillis();
		for (int i = 0; i < size; i++) {
			linkedList.add(i); // 尾部插入
		}
		long linkedListTailInsert = System.currentTimeMillis() - start;
		System.out.println("LinkedList 尾部插入: " +
			linkedListTailInsert + " ms");

		// 测试中部插入
		System.out.println("\n3. 中部插入性能：");

		start = System.currentTimeMillis();
		arrayList.add(arrayList.size() / 2, -1);
		long arrayListMiddleInsert = System.currentTimeMillis() - start;

		start = System.currentTimeMillis();
		linkedList.add(linkedList.size() / 2, -1);
		long linkedListMiddleInsert = System.currentTimeMillis() - start;

		System.out.println("ArrayList 中部插入: " +
			arrayListMiddleInsert + " ms");
		System.out.println("LinkedList 中部插入: " +
			linkedListMiddleInsert + " ms");

		System.out.println("\n【结论】");
		System.out.println("头部插入：LinkedList 远快于 ArrayList");
		System.out.println("尾部插入：两者差不多，ArrayList 略快（可能触发扩容）");
		System.out.println("中部插入：两者都是 O(n)，ArrayList 略快（CPU缓存友好）");
		System.out.println();
	}

	/**
	 * 演示内存占用
	 */
	private static void demonstrateMemoryUsage() {
		System.out.println("【演示5】内存占用对比");
		System.out.println("----------------------------------------");

		System.out.println("ArrayList 内存结构：");
		System.out.println(" Object[] elementData; // 对象引用数组");
		System.out.println(" 内存 = 对象头 + size字段 + elementData引用 + 数组内存");
		System.out.println(" 数组内存 = capacity * 引用大小(4/8 bytes)");
		System.out.println(" 会预留空间（capacity >= size），可能有浪费");

		System.out.println("\nLinkedList 内存结构：");
		System.out.println(" Node<E> first, last; // 头尾指针");
		System.out.println(" 每个节点：item + next + prev = 3个引用");
		System.out.println(" 内存 = 头尾指针 + size * (节点对象头 + 3*引用 + item)");
		System.out.println(" 无预留空间，但每个节点有额外指针开销");

		System.out.println("\n【内存对比】假设存储 n 个 Integer：");
		System.out.println("ArrayList:");
		System.out.println("  - 数组容量通常 >= n（可能有 1.5n）");
		System.out.println("  - 总内存 ≈ 数组容量 * 引用大小");
		System.out.println("  - 元素连续存储，CPU缓存友好");
		System.out.println("LinkedList:");
		System.out.println("  - 每个节点包含：prev + next + item 三个引用");
		System.out.println("  - 总内存 ≈ n * (对象头 + 3*引用 + 对齐填充)");
		System.out.println("  - 通常比 ArrayList 占用更多内存（Nodes开销）");
		System.out.println("  - 元素分散，可能有缓存未命中");

		System.out.println("\n【适用建议】");
		System.out.println("内存敏感 + 查询多：ArrayList");
		System.out.println("增删多 + 内存不敏感：LinkedList");
		System.out.println();
	}

	/**
	 * 演示特性和使用场景
	 */
	private static void demonstrateFeatures() {
		System.out.println("【演示6】特性对比和使用场景");
		System.out.println("----------------------------------------");

		System.out.println("ArrayList 特性：");
		System.out.println(" ✓ 实现了 RandomAccess 接口（支持快速随机访问）");
		System.out.println(" ✓ 占用内存较少（元素连续）");
		System.out.println(" ✗ 插入删除慢（需要移动元素）");
		System.out.println(" ✗ 扩容有性能开销");

		System.out.println("\nLinkedList 特性：");
		System.out.println(" ✓ 实现了 Deque 接口（可作栈和队列）");
		System.out.println(" ✓ 插入删除快（修改指针）");
		System.out.println(" ✓ 无需扩容");
		System.out.println(" ✗ 随机访问慢（需要遍历）");
		System.out.println(" ✗ 占用内存多（节点指针开销）");

		// 演示 Deque 接口
		System.out.println("\n【LinkedList 作为栈和队列】");
		LinkedList<String> deque = new LinkedList<>();

		// 栈操作（后进先出）
		System.out.println("\n栈操作（LIFO）：");
		deque.push("First");  // 等价于 addFirst
		deque.push("Second");
		deque.push("Third");
		System.out.println("push 后: " + deque);
		System.out.println("pop: " + deque.pop());  // 等价于 removeFirst
		System.out.println("pop 后: " + deque);

		// 队列操作（先进先出）
		System.out.println("\n队列操作（FIFO）：");
		deque.clear();
		deque.offer("A");  // 等价于 addLast/offerLast
		deque.offer("B");
		deque.offer("C");
		System.out.println("offer 后: " + deque);
		System.out.println("poll: " + deque.poll());  // 等价于 removeFirst/pollFirst
		System.out.println("poll 后: " + deque);

		System.out.println("\n【使用场景选择】");
		System.out.println("选择 ArrayList：");
		System.out.println("  · 随机访问多（get(index)）");
		System.out.println("  · 尾部插入删除多");
		System.out.println("  · 内存敏感");
		System.out.println("  · 遍历操作多");

		System.out.println("选择 LinkedList：");
		System.out.println("  · 头部/中部插入删除多");
		System.out.println("  · 需要栈或队列功能");
		System.out.println("  · 实现 LRU Cache 等");
		System.out.println("  · 元素数量变化大（无需扩容）");

		System.out.println("默认推荐：ArrayList（大部分场景性能更好）");
		System.out.println();
	}

	/**
	 * 演示迭代器性能
	 */
	private static void demonstrateIteratorPerformance() {
		System.out.println("【演示7】遍历方式性能对比");
		System.out.println("----------------------------------------");

		int size = 50000;
		List<Integer> arrayList = new ArrayList<>(size);
		List<Integer> linkedList = new LinkedList<>();

		for (int i = 0; i < size; i++) {
			arrayList.add(i);
			linkedList.add(i);
		}

		System.out.println("数据量: " + size);

		// 1. for 循环遍历 ArrayList（快）
		long start = System.currentTimeMillis();
		for (int i = 0; i < arrayList.size(); i++) {
			Integer num = arrayList.get(i);
		}
		long arrayListForLoop = System.currentTimeMillis() - start;
		System.out.println("\nArrayList for循环遍历: " +
			arrayListForLoop + " ms (推荐)");

		// 2. for-each 遍历 ArrayList（编译成迭代器，略慢）
		start = System.currentTimeMillis();
		for (Integer num : arrayList) {
			// do nothing
		}
		long arrayListForEach = System.currentTimeMillis() - start;
		System.out.println("ArrayList for-each遍历: " +
			arrayListForEach + " ms");

		// 3. 迭代器遍历 LinkedList（推荐，快）
		start = System.currentTimeMillis();
		Iterator<Integer> it = linkedList.iterator();
		while (it.hasNext()) {
			Integer num = it.next();
		}
		long linkedListIterator = System.currentTimeMillis() - start;
		System.out.println("\nLinkedList 迭代器遍历: " +
			linkedListIterator + " ms (推荐)");

		// 4. for 循环遍历 LinkedList（极慢！）
		start = System.currentTimeMillis();
		for (int i = 0; i < linkedList.size(); i++) {
			Integer num = linkedList.get(i); // 每次 O(n)
		}
		long linkedListForLoop = System.currentTimeMillis() - start;
		System.out.println("LinkedList for循环遍历: " +
			linkedListForLoop + " ms (不推荐！O(n²))");

		System.out.println("\n【遍历最佳实践】");
		System.out.println("ArrayList：for循环或for-each都可以");
		System.out.println("LinkedList：必须使用迭代器或for-each（编译成迭代器）");
		System.out.println("绝对不要用 get(index) 遍历 LinkedList！");
		System.out.println();
	}
}
