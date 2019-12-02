package cn.javadog.sd.mybatis.support.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import cn.javadog.sd.mybatis.support.exceptions.ReflectionException;
import cn.javadog.sd.mybatis.support.reflection.invoker.GetFieldInvoker;
import cn.javadog.sd.mybatis.support.reflection.invoker.Invoker;
import cn.javadog.sd.mybatis.support.reflection.invoker.MethodInvoker;
import cn.javadog.sd.mybatis.support.reflection.invoker.SetFieldInvoker;
import cn.javadog.sd.mybatis.support.reflection.property.PropertyNamer;
import cn.javadog.sd.mybatis.support.reflection.resolver.TypeParameterResolver;

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
	 * 可读属性数组，可读就是有get入口，不管是否private
	 */
	 private final String[] readablePropertyNames;

	/**
	 * 可写属性数组，可写就是有set入口，不管是否private
	 */
	 private final String[] writeablePropertyNames;

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
	 * 不区分大小写的属性集合，查询属性时用到，其实放个数组也可以，做成map通用性好一点吧
	 * key 不区分大写的的属性名
	 * value 区分大小写的属性名
	 */
	private Map<String, String> caseInsensitivePropertyMap = new HashMap<>();

	/**
	 * 构造函数
	 */
	public Reflector(Class<?> clazz) {
		// 设置对应的类
		type = clazz;
		// 初始化 defaultConstructor
		addDefaultConstructor(clazz);
		// 初始化 getMethods 和 getTypes ，通过遍历 get 方法。note 注意顺序，先初始化的Get方法，因为下面的 addSetMethods 会使用到这个方法的结果 getTypes
		addGetMethods(clazz);
		// 初始化 setMethods 和 setTypes ，通过遍历 setting 方法。
		addSetMethods(clazz);
		// 初始化 getMethods + getTypes 和 setMethods + setTypes ，通过遍历 fields 属性
		addFields(clazz);
		// 初始化 readablePropertyNames、writeablePropertyNames、caseInsensitivePropertyMap 属性
		readablePropertyNames = getMethods.keySet().toArray(new String[getMethods.keySet().size()]);
		writeablePropertyNames = setMethods.keySet().toArray(new String[setMethods.keySet().size()]);
		for (String propName : readablePropertyNames) {
			caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
		}
		for (String propName : writeablePropertyNames) {
			caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
		}
	}

	/**
	 *  获取指定类的默认构造
	 *  note 这里没有做 {@link SecurityManager} 的安全验证，感兴趣的自行谷歌，我是不懂+不感兴趣
	 */
	private void addDefaultConstructor(Class<?> cls) {
		// 获取所有构造，包括public和private的
		Constructor<?>[] declaredConstructors = cls.getDeclaredConstructors();
		for (Constructor<?> constructor : declaredConstructors) {
			// 遍历所有的构造方法，找到无参数的
			if (constructor.getParameterCount() == 0) {
				// 首先设置为可访问的，避免private那种情况
				try {
					constructor.setAccessible(true);
				}catch (Exception e){
					// 如果有异常 defaultConstructor 最终就是null
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
			// 首先排除参数熟练大于0的方法，get方法没有参数
			if (method.getParameterCount() > 0) {
				continue;
			}
			// 以 get 或者 id 方法名开头的方法，且必须有属性名，那么就是get方法
			String name = method.getName();
			if ((name.startsWith("get") && name.length() > 3) || (name.startsWith("is") && name.length() > 2)) {
				// 获得属性
				name = PropertyNamer.methodToProperty(name);
				// 添加到 conflictingGetMethods 中
				addMethodConflict(conflictingGetters, name, method);
			}
		}
		// 解决get冲突，即选择最适合返回类型的get方法
		resolveGetterConflicts(conflictingGetters);
	}

	/**
	 * 始化 setMethods 和 setTypes ，通过遍历 setting 方法
	 */
	private void addSetMethods(Class<?> cls) {
		// 属性与其 setting 方法的映射。
		Map<String, List<Method>> conflictingSetters = new HashMap<>();
		// 获得所有方法
		Method[] methods = getClassMethods(cls);
		// 遍历所有方法
		for (Method method : methods) {
			String name = method.getName();
			// 方法名为 set 开头，并排除 set() 方法
			if (name.startsWith("set") && name.length() > 3) {
				// 参数数量为 1
				if (method.getParameterTypes().length == 1) {
					// 获得属性
					name = PropertyNamer.methodToProperty(name);
					// 添加到 conflictingSetters 中
					addMethodConflict(conflictingSetters, name, method);
				}
			}
		}
		// 解决 setting 冲突方法
		resolveSetterConflicts(conflictingSetters);
	}

	/**
	 * 初始化 getMethods + getTypes 和 setMethods + setTypes ，通过遍历 fields 属性。
	 * 实际是对 addGetMethods 和 addSetMethods 方法的补充，某些字段没有 set/get 方法，通过Set/GetFieldInvoker的方式 去操作字段的值
	 */
	private void addFields(Class<?> clazz) {
		// 获得所有 field 们
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			// 设置 field 可访问
			try {
				field.setAccessible(true);
			} catch (Exception e) {
			}
			if (field.isAccessible()) {
				// 添加到 setMethods 和 setTypes 中
				if (!setMethods.containsKey(field.getName())) {
					// final+static 类型的字段只能够被classloader修改，我们只处理没有final+static的字段
					int modifiers = field.getModifiers();
					if (!(Modifier.isFinal(modifiers) && Modifier.isStatic(modifiers))) {
						addSetField(field);
					}
				}
				// 添加到 getMethods 和 getTypes 中
				if (!getMethods.containsKey(field.getName())) {
					addGetField(field);
				}
			}
		}
		// 递归，处理父类
		if (clazz.getSuperclass() != null) {
			addFields(clazz.getSuperclass());
		}
	}

	/**
	 * 将字段添加到 setMethods + setTypes，实际针对的是没有 set方法 的字段
	 */
	private void addSetField(Field field) {
		// 判断是合理的属性
		if (isValidPropertyName(field.getName())) {
			// 添加到 setMethods 中
			setMethods.put(field.getName(), new SetFieldInvoker(field));
			// 处理字段的类型
			Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
			// 添加到 setTypes 中
			setTypes.put(field.getName(), typeToClass(fieldType));
		}
	}

	/**
	 * 将字段添加到 setMethods + setTypes，实际针对的是没有 get方法 的字段
	 */
	private void addGetField(Field field) {
		// 判断是合理的属性
		if (isValidPropertyName(field.getName())) {
			// 添加到 getMethods 中
			getMethods.put(field.getName(), new GetFieldInvoker(field));
			// 添加到 getTypes 中
			Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
			getTypes.put(field.getName(), typeToClass(fieldType));
		}
	}

	/**
	 * 获取类的所有方法，包括所有父类的，以及所有private方法
	 */
	private Method[] getClassMethods(Class<?> clazz) {
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
			currentClass = currentClass.getSuperclass();
		}
		// 转换成数组返回
		Collection<Method> methods = uniqueMethods.values();
		// note 养成好的习惯，指定数组的大小
		return methods.toArray(new Method[methods.size()]);
	}

	/**
	 * 将获取到的所有方法与uniqueMethods比对，去重后加入，保证最终拿到一个类所有的方法
	 */
	private void addUniqueMethods(Map<String, Method> uniqueMethods, Method[] methods) {
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
	private String getSignature(Method method) {
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
	 * 源码里面没有
	 * 设置方法可执行，一行代码，包了try-catch而已
	 */
	private void setAccessible(Executable executable) {
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
	 * 解决 setting 冲突方法
	 */
	private void resolveSetterConflicts(Map<String, List<Method>> conflictingSetters) {
		// 遍历每个属性，查找其最匹配的方法。
		// 因为子类可以覆写父类的方法，所以一个属性，可能对应多个 setting 方法
		for (String propName : conflictingSetters.keySet()) {
			List<Method> setters = conflictingSetters.get(propName);
			Class<?> getterType = getTypes.get(propName);
			Method match = null;
			ReflectionException exception = null;
			// 遍历属性对应的 setting 方法
			for (Method setter : setters) {
				Class<?> paramType = setter.getParameterTypes()[0];
				if (paramType.equals(getterType)) {
					// 和 getterType 相同，直接使用，并跳出break
					match = setter;
					break;
				}
				// 这个 exception 来源于上一个 setter 的 pickBetterSetter；
				// note 如果上一次抛错了，除非 paramType.equals(getterType) ，否则最终 match 就是null，程序会抛出错误
				if (exception == null) {
					try {
						// 选择一个更加匹配的
						match = pickBetterSetter(match, setter, propName);
					} catch (ReflectionException e) {
						// 逻辑上讲，抛错了match就是null，不需要再赋值一次
						match = null;
						exception = e;
					}
				}
			}
			// 添加到 setMethods 和 setTypes 中
			if (match == null) {
				throw exception;
			} else {
				addSetMethod(propName, match);
			}
		}
	}

	/**
	 * 解决get冲突，冲突产生的原因是子类重写父类的get方法，当然也有可能来自接口;
	 * note 核心逻辑就是选择返回值最精准（属性范围越小越精准，如ArrayList比List精准）, 不一定是最接近属性（和属性类型一致最接近）类型的get方法；
	 *  比如A类有个 {@link Map} 类型的属性name，父类有个 返回{@link Map} 类型的get方法getName，它自己有一个返回{@link HashMap} 类型的get方法getName，
	 *  实际最终选用的是返回{@link HashMap} 类型的get方法；这也符合jdk的规则，jdk要求子类重写父类或接口的方法时，返回类型必须相同或更精准
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
				Class<?> candidateType = candidate.getReturnType();
				// 类型相同直接报错，因为前面 #getClassMethods 已经保证了方法的unique，出现相同返回类型唯一允许的可能性就是 is/get 方法
				if (winnerType == candidateType) {
					if (!Boolean.class.equals(candidateType)){
						throw new ReflectionException("Illegal overloaded getter method with ambiguous type for property "
							+ propName + " in class " + winner.getDeclaringClass() + ". This breaks the JavaBeans " +
							"specification and can cause unpredictable results.");
					} else if (candidate.getName().startsWith("is")) {
						// 选择 boolean 类型的 is 方法
						winner = candidate;
					}
				} else if (candidateType.isAssignableFrom(winnerType)) {
					// 竞选失败 isAssignableFrom表示 参数class是否可以被调用者所代表，比如狮子狗可以用狗来代表；通俗点将，在这里就是
					// winnerType 是否可以被 candidateType代表；如果可以的话，winnerType更精准，自然candidateType竞选失败
				} else if (winnerType.isAssignableFrom(candidateType)) {
					// 竞选成功
					winner = candidate;
				} else {
					// 两个类风牛马不相及的场景，一个返回狗，一个返回猫，方法都叫 getAnimal，JDK其实都编译不过去
					// note 加此判断从逻辑上说没有意义，还是框架作者也没想那么多？
					throw new ReflectionException("Illegal overloaded getter method with ambiguous type for property "
						+ propName + " in class " + winner.getDeclaringClass() + ". This breaks the JavaBeans " +
						"specification and can cause unpredictable results.");
				}
			}
			// 添加到 getMethods 和 getTypes 中
			addGetMethod(propName, winner);
		}
	}

	/**
	 * 选择更匹配的setter，这里更改了源码中的参数名，更易于理解
	 */
	private Method pickBetterSetter(Method winner, Method candidate, String property) {
		// 如果 winner 为 null，直接将 candidate 作为 winner
		if (winner == null) {
			return candidate;
		}
		// 取方法的第一个参数，set只有一个参数
		Class<?> winnerType = winner.getParameterTypes()[0];
		Class<?> candidateType = candidate.getParameterTypes()[0];
		// 逻辑与方法 resolveGetterConflicts 一样，谁精准用谁
		if (winnerType.isAssignableFrom(candidateType)) {
			return candidate;
		} else if (candidateType.isAssignableFrom(winnerType)) {
			return winner;
		}
		throw new ReflectionException("Ambiguous setters defined for property '" + property + "' in class '"
			+ candidate.getDeclaringClass() + "' with types '" + winnerType.getName() + "' and '"
			+ candidateType.getName() + "'.");
	}

	/**
	 * 将指定属性的set方法添加到 setMethods 和 setTypes
	 */
	private void addSetMethod(String name, Method method) {
		// 排除方法名不合法的
		if (isValidPropertyName(name)) {
			setMethods.put(name, new MethodInvoker(method));
			// 处理 方法的参数类型
			Type[] paramTypes = TypeParameterResolver.resolveParamTypes(method, type);
			// 实际只有一个参数，所以就放第一个
			setTypes.put(name, typeToClass(paramTypes[0]));
		}
	}

	/**
	 * 将属性与方法添加到 getMethods 和 getTypes 中
	 */
	private void addGetMethod(String name, Method method) {
		// 判断属性名是否合法
		if (isValidPropertyName(name)) {
			// 添加到getMethods
			getMethods.put(name, new MethodInvoker(method));
			// 处理方法返回的类型
			Type returnType = TypeParameterResolver.resolveReturnType(method, type);
			// 添加到 getTypes 中
			getTypes.put(name, typeToClass(returnType));
		}
	}

	/**
	 * 将类型转成 class，比如List<String> => List, T => Object
	 */
	private Class<?> typeToClass(Type src) {
		Class<?> result = null;
		if (src instanceof Class) {
			// 普通类型，直接使用类
			result = (Class<?>) src;
		} else if (src instanceof ParameterizedType) {
			// 泛型类型，使用泛型，如 List<Student>, 取RawType就是List
			result = (Class<?>) ((ParameterizedType) src).getRawType();
		} else if (src instanceof GenericArrayType) {
			// 带有泛型的数组，获得具体类，如 T[]，List<T>[]，List<String>[]
			Type componentType = ((GenericArrayType) src).getGenericComponentType();
			if (componentType instanceof Class) {
				// 普通类型，返回指定类型的数组
				result = Array.newInstance((Class<?>) componentType, 0).getClass();
			} else {
				// 递归该方法，返回类
				Class<?> componentClass = typeToClass(componentType);
				result = Array.newInstance((Class<?>) componentClass, 0).getClass();
			}
		}
		// 都不符合，使用 Object 类, 如T[]，首先是 GenericArrayType，转成componentType=T，递归调用后就是Object
		if (result == null) {
			result = Object.class;
		}
		return result;
	}

	/**
	 *  判断属性名是否合法, 排除特殊的符合和序列化用的属性，注意 逻辑运算符'!'在对外面
	 *  coder不是闲得慌也不会这么命名
	 */
	private boolean isValidPropertyName(String name) {
		return !( name.startsWith("$") || "serialVersionUID".equals(name) || "class".equals(name) );
	}

	/**
	 * 获取指定属性的 get方法 的返回类型
	 */
	public Class<?> getGetterType(String propertyName) {
		Class<?> clazz = getTypes.get(propertyName);
		if (clazz == null) {
			throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
		}
		return clazz;
	}

	/**
	 * 获取指定属性的 set方法 的返回类型
	 */
	public Class<?> getSetterType(String propertyName) {
		Class<?> clazz = setTypes.get(propertyName);
		if (clazz == null) {
			throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
		}
		return clazz;
	}

	/**
	 * 返回可读的属性的数组
	 */
	public String[] getGetablePropertyNames() {
		return readablePropertyNames;
	}

	/**
	 * 返回可写属性的数组
	 */
	public String[] getSetablePropertyNames() {
		return writeablePropertyNames;
	}

	/**
	 * 获取指定属性的 get方法 的Invoker, 没有会直接抛错
	 */
	public Invoker getGetInvoker(String propertyName) {
		Invoker method = getMethods.get(propertyName);
		if (method == null) {
			throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
		}
		return method;
	}

	/**
	 * 获取指定属性的 set方法 的Invoker, 没有会直接抛错
	 */
	public Invoker getSetInvoker(String propertyName) {
		Invoker method = setMethods.get(propertyName);
		if (method == null) {
			throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
		}
		return method;
	}

	/**
	 * 查询指定属性属否有 set方法
	 */
	public boolean hasSetter(String propertyName) {
		return setMethods.keySet().contains(propertyName);
	}

	/**
	 * 查询指定属性属否有 get方法
	 */
	public boolean hasGetter(String propertyName) {
		return getMethods.keySet().contains(propertyName);
	}

	/**
	 * 获取反射器对应的类
	 */
	public Class<?> getType() {
		return type;
	}

	/**
	 * 查询是否有默认构造
	 */
	public boolean hasDefaultConstructor() {
		return defaultConstructor != null;
	}

	/**
	 * 获取默认构造，没找到会报错，实例化 Reflector 时会通过反射获取默认构造，没获取到就返回了空，在那里并不会报错
	 */
	public Constructor<?> getDefaultConstructor() {
		if (defaultConstructor != null) {
			return defaultConstructor;
		} else {
			throw new ReflectionException("There is no default constructor for " + type);
		}
	}

	/**
	 * 查询指定名称的属性，查询的名称可以不区分大小写
	 */
	public String findPropertyName(String name) {
		return caseInsensitivePropertyMap.get(name.toUpperCase(Locale.ENGLISH));
	}
}



