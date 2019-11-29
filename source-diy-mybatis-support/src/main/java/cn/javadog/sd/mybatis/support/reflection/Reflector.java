package cn.javadog.sd.mybatis.support.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cn.javadog.sd.mybatis.support.reflection.invoker.Invoker;
import cn.javadog.sd.mybatis.support.reflection.property.PropertyNamer;

/**
 * @author 余勇
 * @date 2019年11月30日 00:28:00
 * 反射器，每个Reflector对应一个类。或缓存操作这个类所有需要的各种信息。例如：构造方法，属性，set/get方法等待
 * todo:
 * 		1. 验证使用反射缓存，是否真的能提升效率，能提升多少
 */
public class Reflector {

	/**
	 * 对应的类
	 */
	private final Class<?> type;

	/**
	 * 可读属性数组
	 */
	// private final String[] readablePropertyNames;

	/**
	 * 可写属性数组
	 */
	// private final String[] writeablePropertyNames;

	/**
	 * 属性对应set方法集合
	 * key 为属性名称
	 * value 为对应的调用器
	 */
	private final Map<String, Invoker> setMethods = new HashMap<>();

	/**
	 * 属性对应get方法集合
	 * key 为属性名称
	 * value 为对应的调用器
	 */
	private final Map<String, Invoker> getMethods = new HashMap<>();

	/**
	 * 属性对应的set方法参数集合
	 * key 为属性名称
	 * value 为对应的参数的类型
	 */
	private final Map<String, Class<?>> setTypes = new HashMap<>();

	/**
	 * 属性对应的get方法参数集合
	 * key 为属性名称
	 * value 为对应的返回值的类型
	 */
	private final Map<String, Class<?>> getTypes = new HashMap<>();

	/**
	 * 默认构造
	 */
	private Constructor<?> defaultConstructor;

	/**
	 * 不区分大小写的属性集合
	 * todo key-value分别对应什么？
	 */
	private Map<String, String> caseInsentivePropertyMap = new HashMap<>();

	public Reflector(Class<?> clazz) {
		// 设置对应的类
		type = clazz;
		// 初始化 defaultConstructor
		addDefaultConstructor(clazz);
		// 初始化 getMethods 和 getTypes
		addGetMethods(clazz);
	}

	/**
	 *  获取指定类的默认构造
	 *  note 这里没有做 {@link SecurityManager} 的安全验证，感兴趣的自行谷歌，我是不懂+不感兴趣
	 */
	public void addDefaultConstructor(Class<?> cls) {
		// 获取所有构造，包括public和private的
		Constructor<?>[] declaredConstructors = cls.getDeclaredConstructors();
		for (Constructor<?> constructor : declaredConstructors) {
			// 遍历所有的构造方法，找到无参数的
			if (constructor.getParameterCount() == 0) {
				// 首先设置为可访问的，避免private那种情况
				try {
					constructor.setAccessible(true);
				}catch (Exception e){
					// 关于反射的异常，我就照搬的代码包一下，毕竟咱也模拟不出怎么出现异常
				}
			}
			// 如果构造方法可以访问，也就是上面没有出现异常，那么就赋值为defaultConstructor
			if (constructor.isAccessible()){
				this.defaultConstructor = constructor;
			}
		}
	}

	/**
	 * 初始化 getMethods 和 getTypes, 提供遍历 get 方法
	 */
	private void addGetMethods(Class<?> cls) {
		// 属性与get方法的映射
		Map<String, List<Method>> conflictingGetters = new HashMap<>();
		// 首先获得所有方法
		Method[] methods = getClassMethods(cls);
		// 遍历所有方法
		for (Method method : methods) {
			// 首先排除参数熟练大于0的方法
			if (method.getParameterCount() > 0) {
				continue;
			}
			// 以 get 或者 id 方法名开头的方法，且必须有属性名，那么就是get方法
			String name = method.getName();
			if ((name.startsWith("get") && name.length() > 3)
				|| (name.startsWith("is") && name.length() > 2)) {
				// 获得属性
				name = PropertyNamer.methodToProperty(name);
				// 添加到 conflictingGetMethods 中
				addMethodConflict(conflictingGetters, name, method);
				// 解决get冲突，即选择最适合返回类型的get方法
			}
		}
	}


	/**
	 * 获取类的所有方法，包括所有父类的，以及所有private方法
	 */
	public static Method[] getClassMethods(Class<?> clazz) {
		// 每个方法签名与该方法的映射
		Map<String, Method> uniqueMethods = new HashMap<>();
		Class<?> currentClass = clazz;
		// 不断循环类，类的父类，类的父类的父类，直到父类为 Object
		// note mybatis 源码使用的是 currentClass != null && currentClass != Object.class，完全没必要多一次判断
		while (currentClass != Object.class) {
			// 记录当前类定义的方法
			addUniqueMethods(uniqueMethods, currentClass.getDeclaredMethods());
			// 记录接口中定义的方法，接口方法的优先级要低于父类的方法，对比Java8的接口的default方法就可以理解的，
			// 关于接口的default方法可以看下https://blog.csdn.net/wf13265/article/details/79363522
			Class<?>[] interfaces = currentClass.getInterfaces();
			for (Class<?> anInterface : interfaces) {
				// 接口没有private方法，可以直接通过getMethods获取父类所有方法。正好避免了在这里再去处理似有方法与父类方法😄
				addUniqueMethods(uniqueMethods, anInterface.getMethods());
			}
			// 获得父类，再去循坏
			currentClass = currentClass.getDeclaringClass();
		}
		// 转换成数组返回
		Collection<Method> methods = uniqueMethods.values();
		// note 养成好的习惯，指定数组的大小
		return methods.toArray(new Method[methods.size()]);
	}

	/**
	 * 将获取到的所有方法与uniqueMethods比对，去重后加入，保证最终拿到一个类所有的方法
	 */
	private static void addUniqueMethods(Map<String, Method> uniqueMethods, Method[] methods) {
		for (Method currentMethod : methods) {
			// 忽略 bridge 方法，
			// note 推荐看下 https://www.zhihu.com/question/54895701/answer/141623158 文章，
			//  指的是jvm对范型擦出时做的取巧，Spring的源码也有不少这种判断
			if (!currentMethod.isBridge()) {
				// 获得方法签名
				String signature = getSignature(currentMethod);
				// 当uniqueMethods不存在时，进行添加，该机制保证了子类重写父类的方法后，此方法添加的method是子类的方法
				if (!uniqueMethods.containsKey(signature)) {
					// 设置方法可访问
					setAccessible(currentMethod);
					// 添加到uniqueMethods
					uniqueMethods.put(signature, currentMethod);
				}
			}
		}
	}

	/**
	 * 获取方法的签名
	 * 就拿当前方法举例，返回值应为 java.lang.String#getSignature:java.lang.reflect.Method
	 */
	private static String getSignature(Method method) {
		StringBuilder sb = new StringBuilder();
		// 返回类型
		Class<?> returnType = method.getReturnType();
		if (returnType != null) {
			sb.append(returnType.getName()).append("#");
		}
		// 方法名
		sb.append(method.getName());
		// 方法参数类型
		Class<?>[] parameterTypes = method.getParameterTypes();
		for (int i = 0; i < parameterTypes.length; i++) {
			sb.append((i == 0) ? ':' : ',').append(parameterTypes[i].getName());
		}
		return sb.toString();
	}

	/**
	 * 设置方法可执行，一行代码，包了try-catch而已
	 */
	private static void setAccessible(Executable executable) {
		try {
			executable.setAccessible(true);
		}catch (Exception e){
			// 关于反射的异常，我就照搬的代码包一下，毕竟咱也模拟不出怎么出现异常
		}
	}

	/**
	 * 将指定属性的方法添加到其对应的列表中，一对多的关系，比如 name 属性可能有多个 getName()，一般是因为重写父类产生的，
	 * 后面会调用 进一步处理
	 */
	private void addMethodConflict(Map<String, List<Method>> conflictingGetMethods, String name, Method method) {
		List<Method> methods = conflictingGetMethods.computeIfAbsent(name, k -> new ArrayList<>());
		methods.add(method);
	}

	/**
	 * 解决get冲突，冲突产生的原因是子类重写父类的get方法，当然也有可能来自接口;
	 * note 核心逻辑就是选择返回值最接近属性类型的get方法，注意不是最精准；比如 A类继承B类，B类继承C类，如果属性是B类型，
	 * 那么即使三个类型的返回值的方法中，C类型更'精准'，但胜利者还是B类型返回值的get方法
	 */
	private void resolveGetterConflicts(Map<String, List<Method>> conflictingGetters) {
		for (Entry<String, List<Method>> entry : conflictingGetters.entrySet()) {
			// 最匹配的方法，也就是最终的胜利者😄
			Method winner = null;
			String propName = entry.getKey();
			for (Method candidate : entry.getValue()) {
				// winner 为空是，说明没有一个人去占领宝座，第一个人直接上
				if (winner == null) {
					winner = candidate;
					continue;
				}
				// 基于返回类型比较
				Class<?> winnerType = winner.getReturnType();
				Class<?> returnType = candidate.getReturnType();
				//
			}
		}
	}
}
