package cn.javadog.sd.mybatis.support.reflection.invoker;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * @author 余勇
 * @date 2019年11月30日 22:51:00
 *
 *  Field 值获取调用者，名字念起来很拗口，哈哈
 */
public class GetFieldInvoker implements Invoker {

	/**
	 * Field 对象
	 */
	private final Field field;

	/**
	 * 构造
	 */
	public GetFieldInvoker(Field field) {
		this.field = field;
	}

	/**
	 * 获得属性
	 */
	@Override
	public Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException {
		return field.get(target);
	}

	/**
	 * 返回属性类型
	 */
	@Override
	public Class<?> getType() {
		return field.getType();
	}

}
