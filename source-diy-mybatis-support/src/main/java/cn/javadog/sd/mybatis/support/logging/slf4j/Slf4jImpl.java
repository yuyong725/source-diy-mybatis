package cn.javadog.sd.mybatis.support.logging.slf4j;

import cn.javadog.sd.mybatis.support.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.spi.LocationAwareLogger;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 *
 * Slf4j 实现类
 */
public class Slf4jImpl implements Log {

  /**
   * 日志打印器
   */
  private Log log;

  /**
   * 构造函数
   * note 如果slf4j的版本<1.6或者得到的log不是LocationAwareLogger类型，就使用Slf4jLoggerImpl的实现，否则使用Slf4jLocationAwareLoggerImpl
   *  的实现。一种策略模式
   */
  public Slf4jImpl(String clazz) {
    // 使用 SLF LoggerFactory 获得 SLF Logger 对象
    Logger logger = LoggerFactory.getLogger(clazz);

    // 如果是 LocationAwareLogger ，则创建 Slf4jLocationAwareLoggerImpl 对象
    if (logger instanceof LocationAwareLogger) {
      try {
        // 检查方法签名，就是通过反射去获取指定参数的方法，这个方法只有版本大于1.6的slf4j才有，报错就使用Slf4jLoggerImpl
        logger.getClass().getMethod("log", Marker.class, String.class, int.class, String.class, Object[].class, Throwable.class);
        log = new Slf4jLocationAwareLoggerImpl((LocationAwareLogger) logger);
        return;
        // 报错就走下面的逻辑👇
      } catch (SecurityException e) {
      } catch (NoSuchMethodException e) {
      }
    }

    // slf4j的版本<1.6或者得到的log不是LocationAwareLogger类型
    log = new Slf4jLoggerImpl(logger);
  }

  /**
   * 打印不同级别的日志
   */
  @Override
  public boolean isDebugEnabled() {
    return log.isDebugEnabled();
  }

  @Override
  public boolean isTraceEnabled() {
    return log.isTraceEnabled();
  }

  @Override
  public void error(String s, Throwable e) {
    log.error(s, e);
  }

  @Override
  public void error(String s) {
    log.error(s);
  }

  @Override
  public void debug(String s) {
    log.debug(s);
  }

  @Override
  public void trace(String s) {
    log.trace(s);
  }

  @Override
  public void warn(String s) {
    log.warn(s);
  }

}
