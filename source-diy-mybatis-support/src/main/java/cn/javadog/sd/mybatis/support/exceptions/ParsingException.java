package cn.javadog.sd.mybatis.support.exceptions;

/**
 * @author 余勇
 * @date 2019年11月29日 15:34:00
 * XML解析异常
 * todo：
 * 		1、源码中在此类的基础上，抽离了一个框架异常的基类，是否也如此操作，暂时保留
 */
public class ParsingException extends BaseException{

	public ParsingException(String message, Throwable cause){
		super(message, cause);
	}

}
