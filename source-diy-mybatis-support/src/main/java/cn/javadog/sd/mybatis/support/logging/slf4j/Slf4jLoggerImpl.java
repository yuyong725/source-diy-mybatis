package cn.javadog.sd.mybatis.support.logging.slf4j;

import cn.javadog.sd.mybatis.support.logging.Log;
import org.slf4j.Logger;

/**
 * @author Eduardo Macarron
 */
/**
 * @author 余勇
 * @date 2019-12-03 20:34
 * slf4j 版本的日志实现，如果slf4j的版本<1.6或者通过LogFactory得到的log不是LocationAwareLogger类型，就使用这种
 */
class Slf4jLoggerImpl implements Log {

  /**
   * 日志打印器
   */
  private final Logger log;

  /**
   * 使用slf4j的日志打印器
   */
  public Slf4jLoggerImpl(Logger logger) {
    log = logger;
  }

  /**
   * 打印不同级别的日志，本质是使用初始化时传进来的 slf4j 的 'Logger'
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
