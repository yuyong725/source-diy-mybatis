package cn.javadog.sd.mybatis.support.reflection.property;

import java.lang.reflect.Field;

/**
 * @author 余勇
 * @date 2019年11月30日 22:31:00
 * 属性复制器
 */
public class PropertyCopier {

	/**
	 * 不让初始化
	 */
	private PropertyCopier() {
	}

	/**
	 * 将 sourceBean 的属性，复制到 destinationBean 中
	 * 框架都有这个类，一般叫 BeanUtils
	 *
	 * @param type 指定类
	 * @param sourceBean 来源 Bean 对象
	 * @param destinationBean 目标 Bean 对象
	 */
	public static void copyBeanProperties(Class<?> type, Object sourceBean, Object destinationBean) {
		Class<?> parent = type;
		// 循环，从当前类开始，不断复制到父类，直到父类不存在
		while (parent != null) {
			// 获得当前 parent 类定义的属性
			final Field[] fields = parent.getDeclaredFields();
			for(Field field : fields) {
				try {
					// 设置属性可访问
					field.setAccessible(true);
					// 从 sourceBean 中，复制到 destinationBean 去
					field.set(destinationBean, field.get(sourceBean));
				} catch (Exception e) {
				}
			}
			// 获得父类
			parent = parent.getSuperclass();
		}
	}

}
