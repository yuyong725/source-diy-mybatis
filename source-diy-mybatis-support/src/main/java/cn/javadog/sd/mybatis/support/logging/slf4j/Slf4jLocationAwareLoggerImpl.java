package cn.javadog.sd.mybatis.support.logging.slf4j;

import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.logging.LogFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.spi.LocationAwareLogger;

/**
 * @author Eduardo Macarron
 */
/**
 * @author 余勇
 * @date 2019-12-03 20:43
 * slf4j版本的实现，如果slf4j的版本>1.6且得到的log是LocationAwareLogger类型,才使用这种
 */
class Slf4jLocationAwareLoggerImpl implements Log {


  /**
   * 这俩不知道啥玩意，貌似是支持一些高级的打印属性
   */
  private static final Marker MARKER = MarkerFactory.getMarker(LogFactory.MARKER);
  private static final String FQCN = Slf4jImpl.class.getName();

  /**
   * 日志打印器
   */
  private final LocationAwareLogger logger;

  /**
   * 构造
   */
  Slf4jLocationAwareLoggerImpl(LocationAwareLogger logger) {
    this.logger = logger;
  }

  /**
   * 打印不同级别的日志
   */
  @Override
  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  @Override
  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  @Override
  public void error(String s, Throwable e) {
    logger.log(MARKER, FQCN, LocationAwareLogger.ERROR_INT, s, null, e);
  }

  @Override
  public void error(String s) {
    logger.log(MARKER, FQCN, LocationAwareLogger.ERROR_INT, s, null, null);
  }

  @Override
  public void debug(String s) {
    logger.log(MARKER, FQCN, LocationAwareLogger.DEBUG_INT, s, null, null);
  }

  @Override
  public void trace(String s) {
    logger.log(MARKER, FQCN, LocationAwareLogger.TRACE_INT, s, null, null);
  }

  @Override
  public void warn(String s) {
    logger.log(MARKER, FQCN, LocationAwareLogger.WARN_INT, s, null, null);
  }

}
