package cn.javadog.sd.mybatis.support.reflection;

import cn.javadog.sd.mybatis.support.exceptions.BaseException;

/**
 * @author 余勇
 * @date 2019年11月30日 02:29:00
 */
public class ReflectionException extends BaseException {
	public ReflectionException(String message) {
		super(message);
	}

	public ReflectionException(String message, Throwable cause) {
		super(message, cause);
	}
}
