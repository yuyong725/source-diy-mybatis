package cn.javadog.sd.mybatis.support.exceptions;

/**
 * @author: 余勇
 * @date: 2019-12-09 18:26
 *
 * builder模块异常
 */
public class BuilderException extends BaseException {

  public BuilderException() {
    super();
  }

  public BuilderException(String message) {
    super(message);
  }

  public BuilderException(String message, Throwable cause) {
    super(message, cause);
  }

  public BuilderException(Throwable cause) {
    super(cause);
  }
}
