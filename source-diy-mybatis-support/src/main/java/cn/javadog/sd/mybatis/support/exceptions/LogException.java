package cn.javadog.sd.mybatis.support.exceptions;


/**
 * @author Clinton Begin
 */
/**
 * @author: 余勇
 * @date: 2019-12-03 20:05
 * 日志异常
 */
public class LogException extends BaseException {

  public LogException() {
    super();
  }

  public LogException(String message) {
    super(message);
  }

  public LogException(String message, Throwable cause) {
    super(message, cause);
  }

  public LogException(Throwable cause) {
    super(cause);
  }

}
