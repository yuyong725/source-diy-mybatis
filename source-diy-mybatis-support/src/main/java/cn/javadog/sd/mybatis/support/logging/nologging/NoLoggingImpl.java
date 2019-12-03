package cn.javadog.sd.mybatis.support.logging.nologging;

import cn.javadog.sd.mybatis.support.logging.Log;

/**
 * @author: 余勇
 * @date: 2019-12-03 20:21
 *
 * 不打印日志的日志，其实就是啥也不干，对的，啥也不干
 */
public class NoLoggingImpl implements Log {

  public NoLoggingImpl(String clazz) {
    // Do Nothing
  }

  @Override
  public boolean isDebugEnabled() {
    return false;
  }

  @Override
  public boolean isTraceEnabled() {
    return false;
  }

  @Override
  public void error(String s, Throwable e) {
    // Do Nothing
  }

  @Override
  public void error(String s) {
    // Do Nothing
  }

  @Override
  public void debug(String s) {
    // Do Nothing
  }

  @Override
  public void trace(String s) {
    // Do Nothing
  }

  @Override
  public void warn(String s) {
    // Do Nothing
  }

}
