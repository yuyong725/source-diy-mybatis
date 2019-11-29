package cn.javadog.sd.mybatis.support.reflection.property;

import java.util.Locale;

import cn.javadog.sd.mybatis.support.reflection.ReflectionException;

/**
 * @author 余勇
 * @date 2019年11月30日 02:25:00
 * 属性名相关测操作方法。
 */
public class PropertyNamer {

	/**
	 * 关闭默认构造
	 */
	public static String methodToProperty(String name) {
		// is,get/set 方法的处理，其他的直接抛错
		if (name.startsWith("is")) {
			name = name.substring(2);
		} else if (name.startsWith("get") || name.startsWith("set")) {
			name = name.substring(3);
		} else {
			throw new ReflectionException("Error parsing property name '" + name + "'. Didn't start with 'is, 'get' " +
				"or 'set'");
		}

		// 首字母小写，这种写法蛮臃肿的，但没必要吹毛求疵！重要的学习到什么，比如Character.isUpperCase这个api
		if (name.length() == 1 || (name.length() > 1) && Character.isUpperCase(name.charAt(1))) {
			name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
		}

		return name;
	}

}
