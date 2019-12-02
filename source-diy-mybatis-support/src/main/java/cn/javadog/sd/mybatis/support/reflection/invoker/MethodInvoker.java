package cn.javadog.sd.mybatis.support.reflection.invoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author 余勇
 * @date 2019年11月30日 14:05:00
 * 方法调用器，用来调用 set/get 方法
 */
public class MethodInvoker implements Invoker {

	/**
	 * 类型
	 */
	private final Class<?> type;

	/**
	 * 指定的方法
	 */
	private final Method method;

	public MethodInvoker(Method method) {
		this.method = method;
		// 参数大小为1时，这种按理是set方法，取第一个参数为类型为type
		if (method.getParameterCount() == 1) {
			type = method.getParameterTypes()[0];
		} else {
			// get方法
			type = method.getReturnType();
		}

	}

	/**
	 *  执行调用
	 */
	@Override
	public Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException {
		return method.invoke(target, args);
	}

	/**
	 * 获取类型，可能是参数类型，也可能是返回值类型等，取决了具体实现类初始化是是 get还是set方法
	 */
	@Override
	public Class<?> getType() {
		return type;
	}
}
