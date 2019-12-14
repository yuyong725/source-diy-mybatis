package cn.javadog.sd.mybatis.support.exceptions;


/**
 * @author 余勇
 * @date 2019-12-04 22:36
 *
 * 类型转换异常
 */
public class TypeException extends BaseException {


  public TypeException() {
    super();
  }

  public TypeException(String message) {
    super(message);
  }

  public TypeException(String message, Throwable cause) {
    super(message, cause);
  }

  public TypeException(Throwable cause) {
    super(cause);
  }

}
