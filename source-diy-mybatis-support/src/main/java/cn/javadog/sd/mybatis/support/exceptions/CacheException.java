package cn.javadog.sd.mybatis.support.exceptions;

/**
 * @author: 余勇
 * @date: 2019-12-04 15:01
 * 缓存异常
 */
public class CacheException extends BaseException {

  public CacheException() {
    super();
  }

  public CacheException(String message) {
    super(message);
  }

  public CacheException(String message, Throwable cause) {
    super(message, cause);
  }

  public CacheException(Throwable cause) {
    super(cause);
  }

}
