package cn.javadog.sd.mybatis.support.reflection.invoker;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * @author 余勇
 * @date 2019年11月30日 22:53:00
 * Field 值设置调用者
 */
public class SetFieldInvoker implements Invoker {
	/**
	 * Field 对象
	 */
	private final Field field;

	/**
	 * 构造
	 */
	public SetFieldInvoker(Field field) {
		this.field = field;
	}

	/**
	 * 设置 Field 属性
	 */
	@Override
	public Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException {
		field.set(target, args[0]);
		return null;
	}

	/**
	 * 返回属性类型
	 */
	@Override
	public Class<?> getType() {
		return field.getType();
	}
}

