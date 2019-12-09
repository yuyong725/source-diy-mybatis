package cn.javadog.sd.mybatis.support.type;

import java.util.HashMap;

import cn.javadog.sd.mybatis.support.exceptions.BindingException;

/**
 * @author 余勇
 * @date 2019年12月09日 22:22:00
 *
 * 源码是MapperMethod的内部类，为了模块分包清晰，抽离出来
 */
public class ParamMap<V> extends HashMap<String, V> {

	@Override
	public V get(Object key) {
		if (!super.containsKey(key)) {
			throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + keySet());
		}
		return super.get(key);
	}

}