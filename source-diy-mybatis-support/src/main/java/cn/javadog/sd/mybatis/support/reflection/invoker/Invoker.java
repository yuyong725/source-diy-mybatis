package cn.javadog.sd.mybatis.support.reflection.invoker;

import java.lang.reflect.InvocationTargetException;

/**
 * @author 余勇
 * @date 2019年11月30日 00:31:00
 * 调用器接口
 */
public interface Invoker {

	/**
	 *  执行调用
	 * @param target 目标，或者说执行调用的对象
	 * @param args 参数
	 * @return 调用的结果
	 */
	Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException;

	/**
	 * 获取类型，可能是参数类型，也可能是返回值类型等，取决了具体实现类的作用
	 */
	Class<?> getType();

}
