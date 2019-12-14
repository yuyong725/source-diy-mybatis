package cn.javadog.sd.mybatis.support.exceptions;


/**
 * @author 余勇
 * @date 2019-12-02 23:03
 * 数据源模块的异常
 */
public class DataSourceException extends BaseException {


  public DataSourceException() {
    super();
  }

  public DataSourceException(String message) {
    super(message);
  }

  public DataSourceException(String message, Throwable cause) {
    super(message, cause);
  }

  public DataSourceException(Throwable cause) {
    super(cause);
  }

}
