package cn.javadog.sd.mybatis.support.logging;

/**
 * @author: 余勇
 * @date: 2019-12-03 20:16
 * MyBatis Log 接口
 */
public interface Log {

  /**
   * 是否支持debug
   */
  boolean isDebugEnabled();

  /**
   * 是否支持trace
   */
  boolean isTraceEnabled();

  /**
   * 打印几种级别的日志
   */
  void error(String s, Throwable e);

  void error(String s);

  void debug(String s);

  void trace(String s);

  void warn(String s);

}
