package cn.javadog.sd.mybatis.support.logging;

import java.lang.reflect.Constructor;

import cn.javadog.sd.mybatis.support.exceptions.LogException;
import cn.javadog.sd.mybatis.support.logging.nologging.NoLoggingImpl;
import cn.javadog.sd.mybatis.support.logging.slf4j.Slf4jImpl;
import cn.javadog.sd.mybatis.support.logging.stdout.StdOutImpl;

/**
 * @author: 余勇
 * @date: 2019-12-03 20:01
 * Log 工厂类
 */
public final class LogFactory {

  /**
   * 用于支持 MARKER 的日志框架，这玩意我也不懂
   */
  public static final String MARKER = "MYBATIS";

  /**
   * 使用的 Log 的构造方法
   */
  private static Constructor<? extends Log> logConstructor;

  /**
   * 逐个尝试，判断使用哪个 Log 的实现类，即初始化 logConstructor 属性
   */
  static {
    // 参数是runnable，jdk8很有意思的写法
    tryImplementation(LogFactory::useSlf4jLogging);
    tryImplementation(LogFactory::useStdOutLogging);
    tryImplementation(LogFactory::useNoLogging);
  }

  /**
   * 关闭默认构造
   */
  private LogFactory() {
  }

  public static Log getLog(Class<?> aClass) {
    return getLog(aClass.getName());
  }

  public static Log getLog(String logger) {
    try {
      return logConstructor.newInstance(logger);
    } catch (Throwable t) {
      throw new LogException("Error creating logger for logger " + logger + ".  Cause: " + t, t);
    }
  }

  /* 设置几种自定义的 Log 实现类 TODO 源码里写着全路径类名的意义是什么？*/

  /**
   * 使用自定义的实现
   */
  public static synchronized void useCustomLogging(Class<? extends Log> clazz) {
    setImplementation(clazz);
  }

  /**
   * 使用slf4j
   */
  public static synchronized void useSlf4jLogging() {
    setImplementation(Slf4jImpl.class);
  }

  /**
   * 使用控制台输出
   */
  public static synchronized void useStdOutLogging() {
    setImplementation(StdOutImpl.class);
  }

  /**
   * 不打印
   */
  public static synchronized void useNoLogging() {
    setImplementation(NoLoggingImpl.class);
  }

  /**
   * 设置实现类
   * TODO 开线程的意义？逻辑上说线程开了，就不能保证优先级
   */
  private static void tryImplementation(Runnable runnable) {
    if (logConstructor == null) {
      try {
        runnable.run();
      } catch (Throwable t) {
        // ignore
      }
    }
  }

  /**
   * 尝试使用指定的 Log 实现类
   */
  private static void setImplementation(Class<? extends Log> implClass) {
    try {
      // 获得参数为 String 的构造方法
      Constructor<? extends Log> candidate = implClass.getConstructor(String.class);
      // 创建 Log 对象
      Log log = candidate.newInstance(LogFactory.class.getName());
      if (log.isDebugEnabled()) {
        log.debug("Logging initialized using '" + implClass + "' adapter.");
      }
      // 创建成功，意味着可以使用，设置为 logConstructor
      logConstructor = candidate;
    } catch (Throwable t) {
      throw new LogException("Error setting Log implementation.  Cause: " + t, t);
    }
  }

}
