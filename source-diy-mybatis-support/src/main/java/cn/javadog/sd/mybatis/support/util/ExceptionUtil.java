package cn.javadog.sd.mybatis.support.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * @author: 余勇
 * @date: 2019-12-01 18:08
 * 异常工具类
 */
public class ExceptionUtil {

  /**
   * 关闭构造
   */
  private ExceptionUtil() {
  }

  /**
   * 去掉异常的包装
   *
   * @param wrapped 被包装的异常
   * @return 去除包装后的异常
   */
  public static Throwable unwrapThrowable(Throwable wrapped) {
    Throwable unwrapped = wrapped;
    while (true) {
      if (unwrapped instanceof InvocationTargetException) {
        unwrapped = ((InvocationTargetException) unwrapped).getTargetException();
      } else if (unwrapped instanceof UndeclaredThrowableException) {
        unwrapped = ((UndeclaredThrowableException) unwrapped).getUndeclaredThrowable();
      } else {
        return unwrapped;
      }
    }
  }

  /**
   * 包装异常成 BaseException
   *
   * @param message 消息
   * @param e 发生的异常
   * @return BaseException
   */
  public static RuntimeException wrapException(String message, Exception e) {
    // TODO ErrorContext
    return null;
    // return new BaseException(ErrorContext.instance().message(message).cause(e).toString(), e);
  }

}
