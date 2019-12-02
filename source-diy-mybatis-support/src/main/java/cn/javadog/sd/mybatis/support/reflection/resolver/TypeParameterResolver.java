package cn.javadog.sd.mybatis.support.reflection.resolver;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

/**
 * @author 余勇
 * @date 2019年11月30日 14:22:00
 * 参数解析器
 * 暴露了3个 静态方法，分别英语解析 field类型， method返回类型， method参数类型
 * todo 卡住了，标记一下
 * note 这个类看起来很长很复杂，没接触过Type的同学可能很头痛，其实逻辑捋一下就很简单，大部分是重复的代码，
 * 	 记住：Type有如下五种：
 * 	 	GenericArrayType（数组类型）：并不是我们工作中所使用的数组String[] 、byte[]（这种都属于Class），而是带有泛型的数组，即T[] 泛型数组、
 * 	 	ParameterizedType（参数化类型）：就是我们平常所用到的泛型List、Map（注意和TypeVariable的区别）、
 * 	 	WildcardType（泛型表达式类型）：例如List< ? extends Number>这种、
 * 	 	TypeVariable（类型变量）：比如List<T>中的T等、
 * 	 	Class（原始/基本类型）：不仅仅包含我们平常所指的类、枚举、数组、注解，还包括基本类型int、float等等
 * 关于type 强烈推荐看一下：
 * @see
 *	<a href="https://blog.csdn.net/f641385712/article/details/88789847">你真的了解Java泛型参数吗？</a>
 */
public class TypeParameterResolver {

	/**
	 * 不让初始化
	 */
	private TypeParameterResolver() {
		super();
	}

	/**
	 * 解析方法的返回类型
	 *
	 * @param method 要解析的方法
	 * @param srcType 源类型
	 */
	public static Type resolveReturnType(Method method, Type srcType) {
		// 获取方法返回类型对应的Type对象
		Type returnType = method.getGenericReturnType();
		// 声明这个方法的类
		Class<?> declaringClass = method.getDeclaringClass();
		// 解析类型
		return resolveType(returnType, srcType, declaringClass);
	}

	/**
	 * 解析方法参数的类型数组
	 *
	 * @return The parameter types of the method as an array of {@link Type}s. If they have type parameters in the declaration,<br>
	 *         they will be resolved to the actual runtime {@link Type}s.
	 */
	public static Type[] resolveParamTypes(Method method, Type srcType) {
		// 获得方法参数类型数组
		Type[] paramTypes = method.getGenericParameterTypes();
		// 定义的类
		Class<?> declaringClass = method.getDeclaringClass();
		// 解析类型们
		Type[] result = new Type[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			result[i] = resolveType(paramTypes[i], srcType, declaringClass);
		}
		return result;
	}

	public static Type resolveFieldType(Field field, Type srcType) {
		// 属性类型
		Type fieldType = field.getGenericType();
		// 定义的类
		Class<?> declaringClass = field.getDeclaringClass();
		// 解析类型
		return resolveType(fieldType, srcType, declaringClass);
	}

	/**
	 * 解析类型
	 *
	 * @param srcType 源类型
	 * @param type 方法返回的类型，note 注意，不一定是与srcType一致，也有可能是srcType的父类或者实现的接口
	 */
	private static Type resolveType(Type type, Type srcType, Class<?> declaringClass) {
		if (type instanceof TypeVariable) {
			// TypeVariable（类型变量）：比如List<T>中的T等
			return resolveTypeVar((TypeVariable<?>)type, srcType, declaringClass);
		} else if (type instanceof ParameterizedType) {
			// ParameterizedType 参数化类型，即泛型；例如：List< T>、Map< K,V>等带有参数化的对象。
			return resolveParameterizedType((ParameterizedType)type, srcType, declaringClass);
		} else if (type instanceof GenericArrayType) {
			// TypeVariable（类型变量）：比如List<T>中的T等
			return resolveGenericArrayType((GenericArrayType)type, srcType, declaringClass);
		} else{
			return type;
		}
	}

	/**
	 * 解析 TypeVariable 类型
	 * 泛型的类型变量，指的是List<T>、Map< K,V>中的T，K，V等值
	 * 举例：类Mapper<T>, 有一个属性 List<T> results, 有一个对应的get方法 'List<T> getResults()'
	 *
	 * @param typeVar T
	 * @param srcType Mapper<T extends Model & Serializable>
	 * @param declaringClass Mapper
	 */
	private static Type resolveTypeVar(TypeVariable<?> typeVar,  Type srcType, Class<?> declaringClass) {
		Type result = null;
		// 源类型对应的类，如果是ParameterizedType，就去掉皮
		Class<?> clazz = null;
		// note 源类型必须是class或者srcType， 因为这个源类型实际就是我们Reflector所表示的类
		if (srcType instanceof Class) {
			clazz = (Class<?>) srcType;
		} else if (srcType instanceof ParameterizedType) {
			// 如果源类型是ParameterizedType，如List<?>,  那么取rawType也就是List赋值给clazz
			ParameterizedType parameterizedType = (ParameterizedType) srcType;
			clazz = (Class<?>) parameterizedType.getRawType();
		} else {
			throw new IllegalArgumentException("The 2nd arg must be Class or parameterizedType, but was: " + srcType.getClass());
		}

		// 如果 声明方法/字段的类 就是 srcType
		if (clazz == declaringClass) {
			// 如上面的例子，bounds就是 [Model, Serializable], 其中前面的model是类，后面的可能有多个，都是接口
			Type[] bounds = typeVar.getBounds();
			if (bounds.length > 0) {
				//  取第一个返回
				return bounds[0];
			}
			// 没有继承关系的话，直接返回Object
			return Object.class;
		}

		// 如果不相等，也有可能 方法/字段 是在父类/接口中声明的
		// 获取父类
		Type superclass = clazz.getGenericSuperclass();

		result = scanSuperTypes(typeVar, srcType, declaringClass, clazz, superclass);
		if (result != null) {
			return result;
		}

		Type[] superInterfaces = clazz.getGenericInterfaces();
		for (Type superInterface : superInterfaces) {
			result = scanSuperTypes(typeVar, srcType, declaringClass, clazz, superclass);
			if (result != null) {
				return result;
			}
		}

		return Object.class;
	}

	/**
	 * 解析 ParameterizedType 类型
	 * ParameterizedType（参数化类型）：就是我们平常所用到的泛型List、Map（注意和TypeVariable的区别）
	 */
	private static Type resolveParameterizedType(ParameterizedType parameterizedType,  Type srcType,
		Class<?> declaringClass) {
		// 获取类的原始类型，一般都是Class, <> 前面的类型，如List
		Class rawType = (Class<?>)parameterizedType.getRawType();
		// 获取类型内部的参数化类型 比如Map<K,V>里面的K，V类型
		Type[] typeArgs = parameterizedType.getActualTypeArguments();

		Type[] args = new Type[typeArgs.length];
		// 这段逻辑与 resolveType 逻辑一致，就是分情况处理 类似 Map<K,V>的属性类型，然后设置到actualTypeArguments里面
		for (int i = 0; i < typeArgs.length; i++) {
			if (typeArgs[i] instanceof TypeVariable) {
				args[i] = resolveTypeVar((TypeVariable<?>)typeArgs[i], srcType, declaringClass);
			} else if (typeArgs[i] instanceof ParameterizedType) {
				// TypeVariable（类型变量）：比如List<T>中的T等
				args[i] = resolveParameterizedType((ParameterizedType)typeArgs[i], srcType, declaringClass);
			} else if (typeArgs[i] instanceof WildcardType) {
				// TypeVariable（类型变量）：比如List<T>中的T等
				args[i] = resolveWildcardType((WildcardType)typeArgs[i], srcType, declaringClass);
			} else{
				args[i] = typeArgs[i];
			}
		}
		// 创建ParameterizedTypeImpl对象返回
		return new ParameterizedTypeImpl(rawType, null, args);
	}

	/**
	 * 解析 WildcardType 类型
	 */
	private static Type resolveWildcardType(WildcardType wildcardType, Type srcType, Class<?> declaringClass) {
		// <1.1> 解析泛型表达式下界（下限 super）
		Type[] lowerBounds = resolveWildcardTypeBounds(wildcardType.getLowerBounds(), srcType, declaringClass);
		// <1.2> 解析泛型表达式上界（上限 extends）
		Type[] upperBounds = resolveWildcardTypeBounds(wildcardType.getUpperBounds(), srcType, declaringClass);
		// <2> 创建 WildcardTypeImpl 对象
		return new WildcardTypeImpl(lowerBounds, upperBounds);
	}

	private static Type resolveGenericArrayType(GenericArrayType genericArrayType, Type srcType, Class<?> declaringClass) {
		// 【1】解析 componentType
		Type componentType = genericArrayType.getGenericComponentType();
		Type resolvedComponentType = null;
		if (componentType instanceof TypeVariable) {
			resolvedComponentType = resolveTypeVar((TypeVariable<?>) componentType, srcType, declaringClass);
		} else if (componentType instanceof GenericArrayType) {
			resolvedComponentType = resolveGenericArrayType((GenericArrayType) componentType, srcType, declaringClass);
		} else if (componentType instanceof ParameterizedType) {
			resolvedComponentType = resolveParameterizedType((ParameterizedType) componentType, srcType, declaringClass);
		}
		// 【2】创建 GenericArrayTypeImpl 对象
		if (resolvedComponentType instanceof Class) {
			return Array.newInstance((Class<?>) resolvedComponentType, 0).getClass();
		} else {
			return new GenericArrayTypeImpl(resolvedComponentType);
		}
	}

	/**
	 * 扫描父类的类型
	 * 同样举例假如父类的Type是 Service<T extends Model & Serializable>，源类型是Mapper<T extends Model & Serializable>
	 *
	 * @param typeVar T
	 * @param srcType Mapper<T extends Model & Serializable>
	 * @param superClass Service
	 * @param clazz srcType去皮的类，也就是Mapper
	 */
	private static Type scanSuperTypes(TypeVariable<?> typeVar, Type srcType, Class<?> declaringClass, Class<?> clazz
		, Type superClass) {
		// 如果父类是ParameterizedType，Service<T extends Model & Serializable> 就是这种类型
		if (superClass instanceof ParameterizedType) {
			// 首先将父类强转成 ParameterizedType，赋值给 parentAsType，后面会用到这种格式的
			ParameterizedType parentAsType = (ParameterizedType) superClass;
			// 取父类的rawType，也就是 Service
			Class<?> parentAsClass = (Class<?>) parentAsType.getRawType();
			// 取父类的TypeParameters，也就是 'E'(一个字母，代表范型, 并不是T)，注意这只是一个值, 且范型已经被擦除！！！
			TypeVariable<? extends Class<?>>[] parentTypeVars = parentAsClass.getTypeParameters();
			// 如果源类型也是ParameterizedType，这里 Mapper<T extends Model & Serializable> 是这种类型
			if (srcType instanceof ParameterizedType) {
				// 去掉parentAsType中 被 srcType明确了的范型，具体逻辑看方法translateParentTypeVars
				parentAsType = translateParentTypeVars((ParameterizedType) srcType, clazz, parentAsType);
			}
			// 如果方法声明类就是父类
			if (declaringClass == parentAsClass) {
				// 遍历父类型的范型
				for (int i = 0; i < parentTypeVars.length; i++) {
					// 找到了
					if (typeVar == parentTypeVars[i]) {
						return parentAsType.getActualTypeArguments()[i];
					}
				}
			}
			if (declaringClass.isAssignableFrom(parentAsClass)) {
				return resolveTypeVar(typeVar, parentAsType, declaringClass);
			}
		} else if (superClass instanceof Class && declaringClass.isAssignableFrom((Class<?>) superClass)) {
			return resolveTypeVar(typeVar, superClass, declaringClass);
		}
		return null;
	}

	/**
	 * 转换 ParameterizedType 类型的父类的 TypeVar
	 * 举例parentTypeArgs是[D,E,F,List]，srcTypeVars是[Set,E,F,Map]，，最终newParentArgs将会是[null,E,F,List]
	 * 相当于srcTypeVars明确了parentTypeArgs的'D'为'Set'，那就重新new一个ParameterizedTypeImpl，只有[null,E,F,List]
	 *
	 * @param srcType 源类型，如 Mapper<T extends Model & Serializable>
	 * @param srcClass 去皮后的源类型，就是 Mapper
	 * @param parentType 父类型，如 Service<T extends Model & Serializable>
	 */
	private static ParameterizedType translateParentTypeVars(ParameterizedType srcType, Class<?> srcClass, ParameterizedType parentType) {
		// 获取父类型内部的参数化类型 ，这里就是 'T', 注意不包含 ' extends Model & Serializable'，这部分会有其他属性去代表
		Type[] parentTypeArgs = parentType.getActualTypeArguments();
		// 获取源类型内部的参数化类型 ，这里就是 'T'
		Type[] srcTypeArgs = srcType.getActualTypeArguments();
		// 取parentTypeArgs的TypeParameters，也就是 'E'(一个字母，代表范型, 并不是T，具体是什么字母取决于源码，如List<E>的自然是E)，注意这只是一个值, 且范型已经被擦除！！！
		TypeVariable<?>[] srcTypeVars = srcClass.getTypeParameters();
		// 初始化一个数组，大小为 父类型内部的参数化类型，通俗的说就是父类的范型 的数量
		Type[] newParentArgs = new Type[parentTypeArgs.length];
		// 标记符 是否没有变化
		boolean noChange = true;

		// 遍历parentTypeArgs
		// 举例parentTypeArgs是[D,E,F,List]，srcTypeVars是[Set,E,F,Map]，最终newParentArgs将会是[null,E,F,List]
		// 最终的效果就是剔除掉子类明确了的范型，比如例子中srcTypeVars明确了parentTypeArgs的'D'为'Set'。只要有一个明确了，就将noChange标记为false，意思就是子类动了
		for (int i = 0; i < parentTypeArgs.length; i++) {
			// 如果arg是TypeVariable类型进一步操作，案例中的 T 就是，但如果 父类是Service<Student>，那么arg就是Student，是明确的class类型，不是TypeVariable
			if (parentTypeArgs[i] instanceof TypeVariable) {
				// 遍历srcTypeVars
				for (int j = 0; j < srcTypeVars.length; j++) {
					// 如果与父类的范型一致，
					if (srcTypeVars[j] == parentTypeArgs[i]) {
						// 标记 noChange 为false
						noChange = false;
						// 保存到newParentArgs，注意角标
						newParentArgs[i] = srcTypeArgs[j];
					}
				}
			} else {
				// 如果不是TypeVariable类型，直接记到parentTypeArgs
				newParentArgs[i] = parentTypeArgs[i];
			}
		}
		// 如果 srcType 没动父类的范型，直接返回 parentType，否则初始化一个 ParameterizedTypeImpl，里面的<> 中的类型为newParentArgs，去掉了动了的范型
		return noChange ? parentType : new ParameterizedTypeImpl((Class<?>)parentType.getRawType(), null, newParentArgs);
	}

	private static Type[] resolveWildcardTypeBounds(Type[] bounds, Type srcType, Class<?> declaringClass) {
		Type[] result = new Type[bounds.length];
		for (int i = 0; i < bounds.length; i++) {
			if (bounds[i] instanceof TypeVariable) {
				result[i] = resolveTypeVar((TypeVariable<?>) bounds[i], srcType, declaringClass);
			} else if (bounds[i] instanceof ParameterizedType) {
				result[i] = resolveParameterizedType((ParameterizedType) bounds[i], srcType, declaringClass);
			} else if (bounds[i] instanceof WildcardType) {
				result[i] = resolveWildcardType((WildcardType) bounds[i], srcType, declaringClass);
			} else {
				result[i] = bounds[i];
			}
		}
		return result;
	}

	/**
	 * ParameterizedType类型实现类
	 * 参数化类型，即泛型；例如：List< T>、Map< K,V>等带有参数化的对象。
	 * 下面的属性等以 List<T> 举例
	 */
	static class ParameterizedTypeImpl implements ParameterizedType {
		/**
		 * <> 前面的类型
		 *
		 * 例如：List
		 */
		private Class<?> rawType;

		/**
		 * 如果这个类型是某个属性所有，则获取这个所有类类型，比如Map.Entry他的所有者就是Map；否则返回null
		 */
		private Type ownerType;

		/**
		 * <> 中的类型
		 *
		 * 例如：T
		 */
		private Type[] actualTypeArguments;

		public ParameterizedTypeImpl(Class rawType, Type ownerType, Type[] actualTypeArguments) {
			super();
			this.rawType = rawType;
			this.ownerType = ownerType;
			this.actualTypeArguments = actualTypeArguments;
		}


		@Override
		public Type[] getActualTypeArguments() {
			return actualTypeArguments;
		}

		@Override
		public Type getRawType() {
			return rawType;
		}

		@Override
		public Type getOwnerType() {
			return ownerType;
		}

		/**
		 * 重写toString，打印类相信的信息
		 */
		@Override
		public String toString() {
			return "ParameterizedTypeImpl [rawType=" + rawType + "], ownerType=" + ownerType + "], " +
				"actualTypeArguments=" + Arrays.toString(actualTypeArguments) + "]";
		}
	}

	/**
	 * WildcardType 实现类
	 *
	 * 泛型表达式（或者通配符表达式），即 ? extend Number、? super Integer 这样的表达式。
	 * WildcardType 虽然是 Type 的子接口，但却不是 Java 类型中的一种。
	 */
	static class WildcardTypeImpl implements WildcardType {
		/**
		 * 泛型表达式下界（下限 super）
		 */
		private Type[] lowerBounds;

		/**
		 * 泛型表达式上界（上界 extends）
		 */
		private Type[] upperBounds;

		WildcardTypeImpl(Type[] lowerBounds, Type[] upperBounds) {
			super();
			this.lowerBounds = lowerBounds;
			this.upperBounds = upperBounds;
		}

		@Override
		public Type[] getLowerBounds() {
			return lowerBounds;
		}

		@Override
		public Type[] getUpperBounds() {
			return upperBounds;
		}
	}

	/**
	 * GenericArrayType 实现类
	 *
	 * 泛型数组类型，用来描述 ParameterizedType、TypeVariable 类型的数组；即 List<T>[]、T[] 等；
	 */
	static class GenericArrayTypeImpl implements GenericArrayType {
		/**
		 * 数组元素类型
		 */
		private Type genericComponentType;

		GenericArrayTypeImpl(Type genericComponentType) {
			super();
			this.genericComponentType = genericComponentType;
		}

		@Override
		public Type getGenericComponentType() {
			return genericComponentType;
		}
	}
}
