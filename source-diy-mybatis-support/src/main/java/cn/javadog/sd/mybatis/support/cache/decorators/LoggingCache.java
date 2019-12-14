package cn.javadog.sd.mybatis.support.cache.decorators;

import java.util.concurrent.locks.ReadWriteLock;

import cn.javadog.sd.mybatis.support.cache.Cache;
import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.logging.LogFactory;

/**
 * @author Clinton Begin
 */
/**
 * @author 余勇
 * @date 2019-12-04 17:22
 * 日志装饰器，实际只有 {@link #getObject(Object)} 这一步做了增强
 */
public class LoggingCache implements Cache {

  /**
   * MyBatis Log 对象
   */
  private final Log log;

  /**
   * 委托的 Cache 对象
   */
  private final Cache delegate;

  /**
   * 统计请求缓存的次数
   */
  protected int requests = 0;

  /**
   * 统计命中缓存的次数
   */
  protected int hits = 0;

  /**
   * 构造
   */
  public LoggingCache(Cache delegate) {
    this.delegate = delegate;
    // 获取日志
    this.log = LogFactory.getLog(getId());
  }

  /**
   * 获取缓存标示
   */
  @Override
  public String getId() {
    return delegate.getId();
  }

  /**
   * 获取已经缓存的数量
   */
  @Override
  public int getSize() {
    return delegate.getSize();
  }

  /**
   * 添加缓存
   */
  @Override
  public void putObject(Object key, Object object) {
    delegate.putObject(key, object);
  }

  /**
   * 获取缓存
   * 实际只是这一步打印了日志
   */
  @Override
  public Object getObject(Object key) {
    // 请求次数 ++
    requests++;
    // 获得缓存
    final Object value = delegate.getObject(key);
    // 如果命中缓存，则命中次数 ++
    if (value != null) {
      hits++;
    }
    if (log.isDebugEnabled()) {
      log.debug("Cache Hit Ratio [" + getId() + "]: " + getHitRatio());
    }
    return value;
  }

  //👇几个方法都是直接使用委托的对象做的操作，并没有增强

  /**
   * 移除缓存
   */
  @Override
  public Object removeObject(Object key) {
    return delegate.removeObject(key);
  }

  /**
   * 清除缓存
   */
  @Override
  public void clear() {
    delegate.clear();
  }

  /**
   * 获取读写锁，未做实现
   */
  @Override
  public ReadWriteLock getReadWriteLock() {
    return null;
  }

  /**
   * 获取hashcode
   */
  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  /**
   * 重写equal
   */
  @Override
  public boolean equals(Object obj) {
    return delegate.equals(obj);
  }

  /**
   * @return 命中比率
   */
  private double getHitRatio() {
    return (double) hits / (double) requests;
  }

}
