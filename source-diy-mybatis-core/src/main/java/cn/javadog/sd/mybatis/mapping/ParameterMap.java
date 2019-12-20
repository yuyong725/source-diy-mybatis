package cn.javadog.sd.mybatis.mapping;

import java.util.Collections;
import java.util.List;

import cn.javadog.sd.mybatis.session.Configuration;

/**
 * @author 余勇
 * @date 2019-12-11 19:26
 *
 * 参数集合，这里指的不是如下示例中的xml，那种已被废弃！对于未指定专门指定参数类型与映射关系的，mybatis也会如此
 * 格式如：
 * <parameterMap type="map" id="testParameterMap">
 *   <parameter property="addend1" jdbcType="INTEGER" mode="IN"/>
 *   <parameter property="sum" jdbcType="INTEGER" mode="OUT"/>
 * </parameterMap>
 *
 * 1、无参数的，如查询表数据条数，此时 id 固定为 'defaultParameterMap'，parameterMappings 为空集合
 * 2、单参数，使用 parameterType，此时使用内联参数，id 格式为 '{package}.{mapper}.{method}-inline'，如'org.apache.ibatis.mappers
 * 		.AuthorMapper.selectAuthor-Inline'，parameterMappings 为空集合
 * 3、多参数，使用 parameterMap，这种情况本框架不再支持，移除所有相关逻辑！
 * 4、单/多参数，parameterType/parameterMap 都未指定，甚至直接使用注解的形式标记的SQL，此时也是内联参数，id 格式和 parameterMappings 如情况2
 */
public class ParameterMap {

	/**
	 * 唯一标示
	 */
	private String id;

	/**
	 * 对应的 type 类型
	 */
	private Class<?> type;

	/**
	 * 所有解析的映射关系
	 */
	private List<ParameterMapping> parameterMappings;

	/**
	 * 构造，对外不开放，有👇的构造器调用
	 */
	private ParameterMap() {
	}

	/**
	 * 内部类，ParameterMap 的构造器
	 */
	public static class Builder {

		/**
		 * 要构建的ParameterMap对象，空构造，属性由下面的构造方法设置
		 */
		private ParameterMap parameterMap = new ParameterMap();

		/**
		 * 构造函数
		 */
		public Builder(Configuration configuration, String id, Class<?> type, List<ParameterMapping> parameterMappings) {
			parameterMap.id = id;
			parameterMap.type = type;
			parameterMap.parameterMappings = parameterMappings;
		}

		/**
		 * 获取parameterMap 的type属性
		 */
		public Class<?> type() {
			return parameterMap.type;
		}

		/**
		 * 执行构建
		 */
		public ParameterMap build() {
			//将 parameterMappings 属性锁起来
			parameterMap.parameterMappings = Collections.unmodifiableList(parameterMap.parameterMappings);
			return parameterMap;
		}
	}

	/**
	 * 获取唯一标示
	 */
	public String getId() {
		return id;
	}

	/**
	 * 获取 type
	 */
	public Class<?> getType() {
		return type;
	}

	/**
	 * 获取所有的 ParameterMapping
	 */
	public List<ParameterMapping> getParameterMappings() {
		return parameterMappings;
	}

}