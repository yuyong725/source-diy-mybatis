package cn.javadog.sd.mybatis.support.reflection.factory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import cn.javadog.sd.mybatis.support.exceptions.ReflectionException;

/**
 * @author 余勇
 * @date 2019年11月30日 23:00:00
 * 默认的 ObjectFactory 实现类
 * TODO  序列化接口的实现的作用
 */
public class DefaultObjectFactory implements ObjectFactory {

	@Override
	public <T> T create(Class<T> type) {
		return create(type, null, null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
		// <1> 获得需要创建的类
		Class<?> classToCreate = resolveInterface(type);
		// we know types are assignable
		// <2> 创建指定类的对象
		return (T) instantiateClass(classToCreate, constructorArgTypes, constructorArgs);
	}

	@Override
	public void setProperties(Properties properties) {
	}

	/**
	 * 创建指定类的对象
	 */
	private  <T> T instantiateClass(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
		try {
			Constructor<T> constructor;
			// <x1> 通过无参构造方法，创建指定类的对象
			if (constructorArgTypes == null || constructorArgs == null) {
				constructor = type.getDeclaredConstructor();
				if (!constructor.isAccessible()) {
					constructor.setAccessible(true);
				}
				return constructor.newInstance();
			}
			// <x2> 使用特定构造方法，创建指定类的对象
			constructor = type.getDeclaredConstructor(constructorArgTypes.toArray(new Class[constructorArgTypes.size()]));
			if (!constructor.isAccessible()) {
				constructor.setAccessible(true);
			}
			return constructor.newInstance(constructorArgs.toArray(new Object[constructorArgs.size()]));
		} catch (Exception e) {
			// 拼接 argTypes
			StringBuilder argTypes = new StringBuilder();
			if (constructorArgTypes != null && !constructorArgTypes.isEmpty()) {
				for (Class<?> argType : constructorArgTypes) {
					argTypes.append(argType.getSimpleName());
					argTypes.append(",");
				}
				argTypes.deleteCharAt(argTypes.length() - 1); // remove trailing ,
			}
			// 拼接 argValues
			StringBuilder argValues = new StringBuilder();
			if (constructorArgs != null && !constructorArgs.isEmpty()) {
				for (Object argValue : constructorArgs) {
					argValues.append(String.valueOf(argValue));
					argValues.append(",");
				}
				argValues.deleteCharAt(argValues.length() - 1); // remove trailing ,
			}
			// 抛出 ReflectionException 异常
			throw new ReflectionException("Error instantiating " + type + " with invalid types (" + argTypes + ") or values (" + argValues + "). Cause: " + e, e);
		}
	}

	/**
	 * HashSet和TreeSet的区别
	 * https://blog.csdn.net/coding_1994/article/details/80553554
	 */
	protected Class<?> resolveInterface(Class<?> type) {
		Class<?> classToCreate;
		if (type == List.class || type == Collection.class || type == Iterable.class) {
			classToCreate = ArrayList.class;
		} else if (type == Map.class) {
			classToCreate = HashMap.class;
		} else if (type == SortedSet.class) { // issue #510 Collections Support
			classToCreate = TreeSet.class;
		} else if (type == Set.class) {
			classToCreate = HashSet.class;
		} else {
			classToCreate = type;
		}
		return classToCreate;
	}

	/**
	 * 判断指定类是否为集合类
	 */
	@Override
	public <T> boolean isCollection(Class<T> type) {
		return Collection.class.isAssignableFrom(type);
	}


}
