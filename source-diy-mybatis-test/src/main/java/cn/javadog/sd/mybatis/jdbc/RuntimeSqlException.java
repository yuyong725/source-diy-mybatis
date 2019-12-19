package cn.javadog.sd.mybatis.jdbc;

/**
 * @author 余勇
 * @date 2019-12-18 14:01
 *
 * 允许SQL爆出的异常
 */
public class RuntimeSqlException extends RuntimeException {

  public RuntimeSqlException() {
    super();
  }

  public RuntimeSqlException(String message) {
    super(message);
  }

  public RuntimeSqlException(String message, Throwable cause) {
    super(message, cause);
  }

  public RuntimeSqlException(Throwable cause) {
    super(cause);
  }

}
