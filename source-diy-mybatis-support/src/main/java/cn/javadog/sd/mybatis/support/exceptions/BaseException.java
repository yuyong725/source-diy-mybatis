package cn.javadog.sd.mybatis.support.exceptions;

/**
 * @author 余勇
 * @date 2019年11月29日 15:37:00
 * 所有框架异常的基类，对标源码里的 PersistenceException 和 IbatisException
 */
public class BaseException extends RuntimeException{

	public BaseException(){
		super();
	}

	public BaseException(String message){
		super(message);
	}

	public BaseException(String message, Throwable cause){
		super(message, cause);
	}

	public BaseException(Throwable cause){
		super(cause);
	}

}
