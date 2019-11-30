package cn.javadog.sd.mybatis.support.reflection.factory;

import java.util.List;
import java.util.Properties;

/**
 * @author 余勇
 * @date 2019年11月30日 22:55:00
 * Object 工厂接口，相当强大！用于创建指定类的对象, 必须是类或者集合框架的三个接口 （Set，List，Map）
 */
public interface ObjectFactory {

	/**
	 * 设置 Properties 属性
	 */
	void setProperties(Properties properties);

	/**
	 * 创建指定类的对象，使用默认构造方法
	 */
	<T> T create(Class<T> type);

	/**
	 *  创建指定类的对象，使用特定的构造方法
	 *
	 * @param type 要创建的对象的类型
	 * @param constructorArgTypes 使用的构造函数的参数的类型
	 * @param constructorArgs 使用的构造函数的参数值
	 */
	<T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs);

	/**
	 * 判断指定类是否为集合类，主要目的是为了支持非java语言的集合，比如scala的
	 * TODO 视情况考虑是否删掉
	 */
	<T> boolean isCollection(Class<T> type);

}

