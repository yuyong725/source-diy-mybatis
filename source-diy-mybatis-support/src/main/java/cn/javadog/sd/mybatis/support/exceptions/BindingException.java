package cn.javadog.sd.mybatis.support.exceptions;


/**
 * @author: 余勇
 * @date: 2019-12-09 16:02
 *
 * binding模块的异常
 */
public class BindingException extends BaseException {

  public BindingException() {
    super();
  }

  public BindingException(String message) {
    super(message);
  }

  public BindingException(String message, Throwable cause) {
    super(message, cause);
  }

  public BindingException(Throwable cause) {
    super(cause);
  }
}
