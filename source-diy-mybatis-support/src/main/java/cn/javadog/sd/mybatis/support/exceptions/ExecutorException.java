package cn.javadog.sd.mybatis.support.exceptions;

/**
 * @author: 余勇
 * @date: 2019-12-11 13:49
 * Executor 模块的异常
 */
public class ExecutorException extends BaseException {

  private static final long serialVersionUID = 4060977051977364820L;

  public ExecutorException() {
    super();
  }

  public ExecutorException(String message) {
    super(message);
  }

  public ExecutorException(String message, Throwable cause) {
    super(message, cause);
  }

  public ExecutorException(Throwable cause) {
    super(cause);
  }

}
