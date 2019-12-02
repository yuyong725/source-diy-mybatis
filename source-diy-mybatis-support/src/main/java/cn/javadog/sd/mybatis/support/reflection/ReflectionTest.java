package cn.javadog.sd.mybatis.support.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author 余勇
 * @date 2019年11月30日 18:56:00
 */
public class ReflectionTest<T,D extends List> extends ArrayList {

	private List<T> tList;

	private List<Reflector> reflectors;
	private ReflectionTest tt;

	public static void main(String[] args) {
		Field[] fields = ReflectionTest.class.getDeclaredFields();
		for (Field field : fields) {
			printField(field);
		}
	}

	public static void printField(Field field) {
		StringBuilder sb = new StringBuilder();
		sb.append(field.getName())
			.append(" => ")
			.append(" GenericType:")
			.append(field.getGenericType());
		if (field.getGenericType() instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
			Class<?> rawClass = (Class<?>) parameterizedType.getRawType();
			sb.append(" rawClass:")
				.append(Arrays.toString(rawClass.getTypeParameters()))
				.append(" ActualTypeArguments:")
				.append(Arrays.toString(parameterizedType.getActualTypeArguments()));
		}
		System.out.println(sb.toString());

		Class<?> aClass = Array.newInstance(Reflector.class, 8).getClass();
		System.out.println(aClass.getName());
	}

}
