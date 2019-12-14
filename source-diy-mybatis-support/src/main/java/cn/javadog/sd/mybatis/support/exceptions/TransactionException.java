package cn.javadog.sd.mybatis.support.exceptions;

/**
 * @author 余勇
 * @date 2019-12-04 13:14
 * 事务异常
 */
public class TransactionException extends BaseException {

  public TransactionException() {
    super();
  }

  public TransactionException(String message) {
    super(message);
  }

  public TransactionException(String message, Throwable cause) {
    super(message, cause);
  }

  public TransactionException(Throwable cause) {
    super(cause);
  }

}
