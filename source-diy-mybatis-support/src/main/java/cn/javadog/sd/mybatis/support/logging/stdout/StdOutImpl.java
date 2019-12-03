package cn.javadog.sd.mybatis.support.logging.stdout;


import cn.javadog.sd.mybatis.support.logging.Log;

/**
 * @author Clinton Begin
 *
 * 实现 Log 接口，StdOut 实现类，就是直接System.err/out.println日志
 */
public class StdOutImpl implements Log {

  /**
   * 默认构造
   * 控制台打印，所以class拿来也没用
   */
  public StdOutImpl(String clazz) {
  }

  /**
   * 支持debug级别
   */
  @Override
  public boolean isDebugEnabled() {
    return true;
  }

  /**
   * 支持trace级别
   */
  @Override
  public boolean isTraceEnabled() {
    return true;
  }

  /**
   * 使用 System.err/out.println 打印几种级别的日志
   */
  @Override
  public void error(String s, Throwable e) {
    System.err.println(s);
    e.printStackTrace(System.err);
  }

  @Override
  public void error(String s) {
    System.err.println(s);
  }

  @Override
  public void debug(String s) {
    System.out.println(s);
  }

  @Override
  public void trace(String s) {
    System.out.println(s);
  }

  @Override
  public void warn(String s) {
    System.out.println(s);
  }
}
