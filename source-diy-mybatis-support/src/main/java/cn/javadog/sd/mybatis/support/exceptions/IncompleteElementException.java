package cn.javadog.sd.mybatis.support.exceptions;

/**
 * @author 余勇
 * @date 2019-12-09 20:06
 *
 * 解析未完成异常
 */
public class IncompleteElementException extends BuilderException {
  public IncompleteElementException() {
    super();
  }

  public IncompleteElementException(String message, Throwable cause) {
    super(message, cause);
  }

  public IncompleteElementException(String message) {
    super(message);
  }

  public IncompleteElementException(Throwable cause) {
    super(cause);
  }

}
